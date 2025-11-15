package com.bukload.ai.service;

import com.bukload.ai.config.GeminiProperties;
import com.bukload.ai.domain.course.*;
import com.bukload.ai.domain.request.TravelRequest;
import com.bukload.ai.domain.request.TravelRequestRepository;
import com.bukload.ai.dto.request.CreateTravelRequestReq;
import com.bukload.ai.dto.request.RecommendCoursesReq;
import com.bukload.ai.dto.request.SaveCourseReq;
import com.bukload.ai.dto.response.CourseDetailRes;
import com.bukload.ai.dto.response.CourseSummaryRes;
import com.bukload.ai.dto.response.RegionRecommendRes;
import com.bukload.ai.dto.response.SavedCourseDtoForTemp;
import com.bukload.ai.service.llm.GeminiClient;
import com.bukload.ai.service.preset.CoursePresetProvider;
import com.bukload.ai.service.preset.RegionPresetProvider;
import com.bukload.course.dto.PlaceDto;
import com.bukload.localpaychecker.api.LocalpayApiService;
import com.bukload.transit.TransitService;
import com.bukload.auth.user.User;
import com.bukload.transit.client.dto.TransitRouteResponse;
import com.bukload.travel1.dto.PlaceResponse;
import com.bukload.travel1.service.PlaceSearchServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {

    private final TravelRequestRepository travelRequestRepository;
    private final CourseRepository courseRepository;
    private final TempCourseRepository tempCourseRepository;
    private final DistanceCheckClient distanceClient;

    private final GeminiClient geminiClient;
    private final GeminiProperties props;

    // 프리셋: 이제는 "예시 few-shot 데이터" 용도로만 사용
    private final RegionPresetProvider regionPresetProvider;
    private final CoursePresetProvider coursePresetProvider;

    private final PlaceSearchServiceImpl placeSearchService;

    private final TransitService transitService;

    private final LocalpayApiService localpayApiService;



    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Gemini 응답 텍스트에서 순수한 JSON 블록만 추출하여 파싱 안정성 강화
     */
    private String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) return "";
        // 첫 번째 '{'와 마지막 '}'를 찾아 그 사이의 문자열만 추출
        int start = rawText.indexOf('{');
        int end = rawText.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            // '```json' 또는 '```' 마크다운 블록이 있을 경우 시작 위치 조정
            int codeBlockStart = rawText.toLowerCase().indexOf("```");
            if (codeBlockStart != -1 && codeBlockStart < start) {
                // '```json' 다음이나 '```' 다음의 '{'를 다시 찾음
                start = rawText.indexOf('{', codeBlockStart);
            }

            if (start != -1 && end > start) {
                return rawText.substring(start, end + 1).trim();
            }
        }
        return ""; // 유효한 JSON 블록을 찾지 못하면 빈 문자열 반환
    }
    //코스 추천 설문 부분
    public Long createTravelRequest(CreateTravelRequestReq req){
        TravelRequest tr = TravelRequest.builder()
                .themeId(req.getThemeId())
                .departureLocation(req.getDepartureLocation())
                .travelDays(req.getTravelDays())
                .budget(req.getBudget())
                .gender(req.getGender())
                .birthDate(req.getBirthDate())
                .companions(req.getCompanions())
                .style(req.getStyle())
                .additionalRequest(req.getAdditionalRequest())
                .build();
        travelRequestRepository.save(tr);
        return tr.getId();
    }

    /** 지역 우선 추천 (Gemini 실제 응답 보기 버전) */
    public RegionRecommendRes recommendRegion(Long requestId, User user){
        TravelRequest tr = travelRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("request not found"));

        String prompt = """
            너는 한국 여행 코스 추천 전문가야.
            
            아래 사용자의 여행 조건을 보고, 경기도 북부 4개 후보 중에서 가장 적합한 '기초지자체(시/군)' 1곳만 골라.
            
            반드시 다음 네 개 중에서만 선택해:
            - 의정부시 (anchorId: "uijeongbu")
            - 구리시 (anchorId: "guri")
            - 양주시 (anchorId: "yangju")
            - 동두천시 (anchorId: "dongducheon")
            
            반환 형식은 반드시 JSON 한 줄만 사용해.
            설명 문장, 마크다운, 코드 블록(```)은 절대 포함하지 말고, 아래 필드만 넣어:
            
            {
              "region": "...",
              "anchorId": "...",
              "comment": "...",
              "tags": ["...", "..."]
            }
            
            필드 설명:
            - region: 위 후보 중 한국어 '시/군' 이름. 예: "양주시"
            - anchorId: 위에 제시한 영문 슬러그 중 하나. 예: "yangju"
            - comment: 1문장짜리 한국어 코멘트. 누구에게 어떤 여행 스타일에 좋은지 써줘.
            - tags: 한국어 키워드 2~4개. 예: ["자연","카페","감성"]
            
            사용자 정보:
            - 출발지: %s
            - 기간(일): %d
            - 예산(원): %d
            - 성별: %s
            - 생년월일: %s
            - 동행: %s
            - 스타일: %s
            - 추가요청: %s
            """.formatted(
                tr.getDepartureLocation(),
                tr.getTravelDays(),
                tr.getBudget(),
                user.getGender(),
                user.getBirthDate() != null ? user.getBirthDate().toString() : "",
                tr.getCompanions(),
                tr.getStyle(),
                tr.getAdditionalRequest()
        );

        String rawJson = null;

        try {
            // ⭐⭐⭐ 503/429 오류 자동 재시도 적용 ⭐⭐⭐
            rawJson = callGeminiWithRetry(() ->
                    geminiClient.generate(props.getModelRegion(), prompt).block()
            );

        } catch (Exception e) {
            System.out.println("[RecommendationService] ❗ Gemini API 호출 실패: " + e.getMessage());
            // 이후 fallback으로 넘어감
        }

        // 2) JSON 추출 및 파싱
        if (rawJson != null && !rawJson.isBlank()) {
            String json = extractJson(rawJson);

            if (!json.isBlank()) {
                try {
                    JsonNode node = mapper.readTree(json);

                    String region = node.path("region").asText();
                    String anchorId = node.path("anchorId").asText();
                    String comment = node.path("comment").asText("추천 지역입니다.");
                    List<String> tags = mapper.convertValue(node.path("tags"), List.class);

                    // DB 저장
                    tr.setRegionName(region);
                    tr.setAnchorId(anchorId);
                    travelRequestRepository.save(tr);

                    return RegionRecommendRes.builder()
                            .region(region)
                            .anchorId(anchorId)
                            .comment(comment)
                            .tags(tags)
                            .build();

                } catch (Exception e) {
                    System.out.println("[RecommendationService] ⚠️ JSON 파싱 실패 → fallback 사용: " + e.getMessage());
                }
            }
        }

        // 3) Fallback
        final String fallbackRegion = "양평군";
        final String fallbackAnchor = "yangpyeong";

        tr.setRegionName(fallbackRegion);
        tr.setAnchorId(fallbackAnchor);
        travelRequestRepository.save(tr);

        return RegionRecommendRes.builder()
                .region(fallbackRegion)
                .anchorId(fallbackAnchor)
                .comment("감성 여행지 (Fallback)")
                .tags(List.of("자연","카페"))
                .build();
    }


    /**
     * ✅ 코스 추천 및 임시 저장
     * - Gemini로부터 AI 추천 결과를 받아 TempCourse에 임시 저장
     * - 반환: SavedCourseDtoForTemp 리스트 (프론트 표시용)
     * - (JWT 사용자 기반)
    /**

     */
    public List<SavedCourseDtoForTemp> recommendCourses(RecommendCoursesReq req, User user) {
        // ✅ 1️⃣ TravelRequest 조회
        TravelRequest tr = travelRequestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new IllegalArgumentException("request not found"));

        // ✅ 2️⃣ 로그인된 사용자 ID 추출
        Long userId = user.getId();

        // ✅ 3️⃣ 지역, 앵커 설정
        String region = Optional.ofNullable(tr.getRegionName()).orElse("양평군");
        String anchor = Optional.ofNullable(req.getAnchorId())
                .orElse(Optional.ofNullable(tr.getAnchorId()).orElse("yangpyeong"));


        // ✅ 추가) 프리셋을 "예시 JSON"으로 만들어 few-shot 컨텍스트에 넣기
        String exampleSection;
        try {
            List<CourseSummaryRes.CourseCard> exampleCards = coursePresetProvider.findByAnchor(anchor);
            if (exampleCards != null && !exampleCards.isEmpty()) {
                CourseSummaryRes example = CourseSummaryRes.builder()
                        .region(region)
                        .courses(exampleCards)
                        .build();
                String exampleJson = mapper.writeValueAsString(example);
                exampleSection = """
                        아래는 이 지역/인근 지역에서 우리가 과거에 사용했던 예시 코스 JSON이야.
                        형식과 설명 톤, 장소 구성 방식을 참고만 하고,
                        장소 이름, 설명, 동선 구성은 새롭게 만들어.

                        [예시 JSON - 복사 금지, 참고용]
                        %s

                        [예시 JSON 끝]
                        """.formatted(exampleJson);
            } else {
                exampleSection = "예시 코스 JSON은 따로 제공하지 않는다. 너가 스키마만 잘 지켜서 새 코스를 설계해.";
            }
        } catch (Exception e) {
            System.out.println("[RecommendationService] ⚠️ 예시 JSON 직렬화 실패: " + e.getMessage());
            exampleSection = "예시 코스 JSON은 직렬화 오류로 제공되지 않는다. 스키마만 참고해서 새 코스를 설계해.";
        }

        // ✅ 4️⃣ Gemini 프롬프트 생성 (동일)
        String prompt = """
                너는 한국 로컬 여행 코스 설계자야.

                지역: %s (앵커: %s)

                아래 사용자의 프로필을 보고, 해당 지역 안에서 3개 이상의 코스 후보를 설계해.

                %s

                ⚠️ 출력 형식 규칙 (중요)
                - 반드시 아래 JSON 스키마와 동일한 구조로만 출력해.
                - JSON 이외의 설명 문장, 마크다운, 코드 블록( ``` )은 절대 넣지 마.
                - 모든 텍스트는 한국어로 작성해.

                반환 스키마 예시는 다음과 같아 (값은 예시일 뿐, 형식만 따라 해):
                {
                  "region": "%s",
                  "courses": [
                    {
                      "title": "코스 제목",
                      "description": "이 코스가 누구에게, 어떤 분위기로 좋은지 2문장 이내로 설명.",
                      "places": [
                        { "name": "장소 이름", "category": "카테고리", "lat": 37.5, "lng": 127.3 }
                      ],
                      "totalDistance": "18km",
                      "estimatedTime": "5시간",
                      "tags": ["자연","카페"],
                      "localCurrencyMerchants": 10
                    }
                  ]
                }

                코스/장소 구성 가이드:

                1) 코스 구조
                - 각 코스는 3~5개의 장소로 구성해.
                - 이동 동선이 자연스럽게 이어지도록 순서를 잡아.
                - 같은 코스 안에서 너무 멀리 떨어진 장소는 넣지 말고 같은 생활권 위주로 묶어.

                2) 장소(category) 규칙
                - category는 한국어 한 단어 또는 두 단어 정도로 짧게 써.
                  예시: "식당", "카페", "공원", "도서관", "문화시설", "실내액티비티", "시장", "체험"
                - lat, lng는 해당 지역 안에 있을 법한 위도/경도(double) 값으로 작성해.
                - 반드시 실재하는 장소만 선택해야하고, 이전 장소와 너무 멀리 떨어져 있으면 안돼
                - 막 ~~카페 이런 추상적인 장소면 절대로 안되고 상호명을 확실히 알려줘야해 
                - 각 지점마다 대중교통(주로 버스)로 이동하기 쉬운 곳으로 선택해줘 걷는 구간이 없도록 버스정류장 주변에 위치한 곳으로 부탁할게

                3) 설명 톤
                - description에는 이 코스의 특징과 타깃을 써줘.
                - 사용자의 동행 정보에 따라 자연스럽게 톤을 맞춰줘.

                4) tags 규칙
                - tags에는 이 코스를 잘 설명하는 핵심 키워드를 2~4개 넣어.
                - 예시: ["맛집","로컬푸드"], ["감성카페","포토"], ["액티비티","실내놀이"], ["공원","산책"].

                5) 거리/시간
                - totalDistance는 "4km", "7km"처럼 숫자+km 형식 문자열.
                - estimatedTime은 "3시간", "4시간 30분"처럼 한국어 시간 표현.

                사용자 입력:
                - 기간: %d일
                - 예산: %d원
                - 성별: %s
                - 동행: %s
                - 스타일: %s
                - 추가요청: %s
            """.formatted(
                region, anchor, exampleSection,
                region,
                tr.getTravelDays(), tr.getBudget(),
                tr.getGender(), tr.getCompanions(),
                tr.getStyle(), tr.getAdditionalRequest()
        );

        // ✅ 5️⃣ Gemini 호출 (503/429 재시도 포함)
        String rawJson = callGeminiWithRetry(() ->
                geminiClient.generate(props.getModelCourses(), prompt).block()
        );


        // ✅ 6️⃣ JSON 파싱
        String json = extractJson(rawJson);
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            System.out.println("[RecommendationService] ⚠️ 코스 JSON 파싱 실패 → Fallback 사용");
            return List.of();
        }
        JsonNode nodes = root.path("courses");

        // ✅ 7️⃣ 기존 임시 데이터 삭제
        tempCourseRepository.deleteByUserId(userId);

        // ✅ 8️⃣ 변환 리스트
        List<SavedCourseDtoForTemp> dtos = new ArrayList<>();

        // ✅ 9️⃣ 후보별 TempCourse 저장
        if (nodes.isArray() && nodes.size() > 0) {

            // ✅ 기존 TempCourse 완전 초기화 (AUTO_INCREMENT 리셋 포함)
            try {
                tempCourseRepository.deleteByUserId(userId);

                System.out.println("✅ TempCourse 테이블 초기화 완료 (AUTO_INCREMENT 리셋 -> 이건 미구현)");
            } catch (Exception e) {
                System.out.println("[RecommendationService] ⚠️ TempCourse 초기화 실패: " + e.getMessage());
            }

            for (JsonNode n : nodes) {
                try {
                    String placesJson = mapper.writeValueAsString(n.path("places"));

                    TempCourse temp = TempCourse.builder()
                            .userId(userId)
                            .anchorId(anchor)
                            .title(n.path("title").asText())
                            .description(n.path("description").asText())
                            .placesJson(placesJson)

                            // ⭐ 새로 추가한 필드들
                            .regionName(region)                              // TravelRequest의 regionName
                            .travelDays(tr.getTravelDays() + "일")           // "1일", "2일"

                            .createdAt(LocalDateTime.now())
                            .build();

                    tempCourseRepository.save(temp);
                    dtos.add(SavedCourseDtoForTemp.from(temp, tr));

                } catch (Exception e) {
                    System.out.println("[RecommendationService] ⚠️ TempCourse 저장 중 오류: " + e.getMessage());
                }
            }

        }


        // ✅ 10️⃣ 반환
        return dtos;
    }


    /**
     * ✅ 사용자가 선택한 임시 코스를 정식 코스로 저장
     * - 프론트로부터 tempCourseId를 받아 TempCourse 조회
     * - TempCourse의 정보와 placesJson을 실제 Course / CourseSegment로 변환
     * - ODsay api 적용됨
     */
    @Transactional
    public Long saveCourse(Long tempCourseId, User user) {
        // 1️⃣ TempCourse 조회
        TempCourse temp = tempCourseRepository.findById(tempCourseId)
                .orElseThrow(() -> new IllegalArgumentException("tempCourse not found"));

        // 2️⃣ TempCourse → Course 변환
        Course course = Course.builder()

                .user(user)                                 // User
                .anchorId(temp.getAnchorId())               // 앵커 ID
                .title(temp.getTitle())                     // 제목
                .description(temp.getDescription())         // 설명
                .regionName(temp.getRegionName())           // ⭐ 시군명 (이미 temp에 있음)
                .travelDays(temp.getTravelDays())           // ⭐ "1일", "2일" 저장됨!
                .build();


// 3️⃣ placesJson 파싱 → CourseSegment 변환
        try {
            JsonNode places = mapper.readTree(temp.getPlacesJson());
            int order = 1;

            for (int i = 0; i < places.size(); i++) {
                JsonNode current = places.get(i);
                JsonNode next = (i + 1 < places.size()) ? places.get(i + 1) : null;

                String name = current.path("name").asText();
                Double lat = current.path("lat").asDouble();
                Double lng = current.path("lng").asDouble();

                // ⭐ 대표 사진 URL
                String photoUrl = placeSearchService.getRepresentativePhotoUrl(
                        name,
                        null,
                        lat,
                        lng
                );

                // ⭐ 지역명 (시군명)
                String sigunNm = course.getRegionName();

                // ⭐ 로컬페이 사용 여부
                boolean localpayUsable = false;
                try {
                    localpayUsable = localpayApiService
                            .isLocalpayUsable(sigunNm, name)
                            .block();
                } catch (Exception e) {
                    log.warn("Localpay check failed for place {}", name);
                }

                // ⭐ 기본 Segment 생성
                CourseSegment seg = CourseSegment.builder()
                        .course(course)
                        .orderNo(order++)
                        .placeId(current.path("placeId").asText(null))
                        .placeName(name)
                        .category(current.path("category").asText())
                        .lat(lat)
                        .lng(lng)
                        .photoUrl(photoUrl)
                        .localpayOX(localpayUsable)
                        .build();

                // ⭐ 다음 장소가 있다면 — ODsay 경로 추가
                if (next != null) {
                    Double nextLat = next.path("lat").asDouble();
                    Double nextLng = next.path("lng").asDouble();

                    log.info("[ODsay-DEBUG] from=({}, {}), to=({}, {})",
                            lat, lng, nextLat, nextLng);


                    try {
                        TransitRouteResponse route = transitService
                                .searchBestRoute(lng, lat, nextLng, nextLat, 0)
                                .block();

                        if (route != null) {
                            String routeJson = mapper.writeValueAsString(route);
                            seg.setTransitJson(routeJson);
                        }
                    } catch (Exception e) {
                        log.warn("[saveCourse] ODsay 호출 실패: {}", e.getMessage());
                    }
                } else {
                    // 마지막 장소 → transit 없음
                    seg.setTransitJson(null);
                }

                // ⭐ 코스에 segment 추가
                course.getSegments().add(seg);
            }

            // 4️⃣ 저장
            courseRepository.save(course);
            tempCourseRepository.delete(temp);

        } catch (Exception e) {
            log.error("[saveCourse] placesJson 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("saveCourse 실패", e);
        }



        return course.getId(); // ⭐ 최종 생성된 코스 ID 반환

    }



    private CourseSummaryRes.CourseCard dummyCourse(String title){
        List<CourseSummaryRes.PlaceBrief> places = List.of(
                CourseSummaryRes.PlaceBrief.builder().name("두물머리").category("명소").lat(37.58).lng(127.31).build(),
                CourseSummaryRes.PlaceBrief.builder().name("카페 한강뷰").category("카페").lat(37.57).lng(127.32).build(),
                CourseSummaryRes.PlaceBrief.builder().name("양평 전통시장").category("시장").lat(37.49).lng(127.49).build()
        );

        return CourseSummaryRes.CourseCard.builder()
                .title(title)
                .description("카페와 자연 명소를 하루 만에 즐길 수 있는 코스")
                .places(places)
                .totalDistance("18km")
                .estimatedTime("5시간")
                .tags(List.of("자연","카페"))
                .localCurrencyMerchants(12)
                .build();
    }

    private String callGeminiWithRetry(Supplier<String> geminiCall) {

        int maxRetries = 6;        // 최대 6회 시도
        int attempt = 0;

        while (true) {
            try {
                return geminiCall.get();  // 실제 Gemini API 호출
            } catch (Exception e) {

                // 예외 메시지 안에 503 / 429 가 포함된 경우만 재시도
                String msg = e.getMessage() != null ? e.getMessage() : "";

                if (msg.contains("503") || msg.contains("429")) {

                    attempt++;
                    if (attempt > maxRetries) {
                        throw new RuntimeException("Gemini API 재시도 실패 (최대 횟수 초과)", e);
                    }

                    int wait = (int) (2000 * Math.pow(2, attempt - 1));  // 2 → 4 → 8 →...
                    log.warn("⚠️ Gemini API 오류 503/429 → {}번째 재시도 ({} ms 후)", attempt, wait);

                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ignored) {}

                    continue;
                }

                // 다른 예외는 즉시 던짐
                throw e;
            }
        }
    }

}
