package com.bukload.ai.service;

import com.bukload.ai.config.GeminiProperties;
import com.bukload.ai.domain.course.Course;
import com.bukload.ai.domain.course.CourseRepository;
import com.bukload.ai.domain.course.CourseSegment;
import com.bukload.ai.domain.request.TravelRequest;
import com.bukload.ai.domain.request.TravelRequestRepository;
import com.bukload.ai.dto.request.CreateTravelRequestReq;
import com.bukload.ai.dto.request.RecommendCoursesReq;
import com.bukload.ai.dto.request.SaveCourseReq;
import com.bukload.ai.dto.response.CourseDetailRes;
import com.bukload.ai.dto.response.CourseSummaryRes;
import com.bukload.ai.dto.response.RegionRecommendRes;
import com.bukload.ai.service.llm.GeminiClient;
import com.bukload.ai.service.preset.CoursePresetProvider;
import com.bukload.ai.service.preset.RegionPresetProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {

    private final TravelRequestRepository travelRequestRepository;
    private final CourseRepository courseRepository;
    private final DistanceCheckClient distanceClient;

    private final GeminiClient geminiClient;
    private final GeminiProperties props;

    // 프리셋: 이제는 "예시 few-shot 데이터" 용도로만 사용
    private final RegionPresetProvider regionPresetProvider;
    private final CoursePresetProvider coursePresetProvider;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Gemini 응답 텍스트에서 순수 JSON만 추출 */
    private String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) return "";
        int start = rawText.indexOf('{');
        int end = rawText.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            int codeBlockStart = rawText.toLowerCase().indexOf("```");
            if (codeBlockStart != -1 && codeBlockStart < start) {
                start = rawText.indexOf('{', codeBlockStart);
            }
            if (start != -1 && end > start) {
                return rawText.substring(start, end + 1).trim();
            }
        }
        return "";
    }

    /** 여행 요청 생성 */
    public Long createTravelRequest(CreateTravelRequestReq req) {
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

    /** 지역 추천: Gemini + 4개 중 스냅 */
    public RegionRecommendRes recommendRegion(Long requestId) {
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
                tr.getDepartureLocation(), tr.getTravelDays(), tr.getBudget(),
                tr.getGender(), tr.getBirthDate(), tr.getCompanions(),
                tr.getStyle(), tr.getAdditionalRequest()
        );

        String rawJson = null;
        try {
            rawJson = geminiClient.generate(props.getModelRegion(), prompt).block();
        } catch (Exception e) {
            System.out.println("[RecommendationService] ❗ Gemini API 호출 중 예외: " + e.getMessage());
        }

        RegionRecommendRes picked = null;

        if (rawJson != null && !rawJson.isBlank()) {
            String json = extractJson(rawJson);
            if (!json.isBlank()) {
                try {
                    JsonNode node = mapper.readTree(json);
                    String region = node.path("region").asText();
                    String anchorId = node.path("anchorId").asText();
                    picked = regionPresetProvider.snapToSupported(
                            (anchorId != null && !anchorId.isBlank()) ? anchorId : region
                    );
                } catch (Exception e) {
                    System.out.println("[RecommendationService] ⚠️ Gemini 응답 파싱 실패 -> 프리셋으로 전환: " + e.getMessage());
                }
            }
        }

        if (picked == null) {
            picked = regionPresetProvider.snapToSupported(null);
        }

        tr.setRegionName(picked.getRegion());
        tr.setAnchorId(picked.getAnchorId());
        travelRequestRepository.save(tr);

        return picked;
    }

    /**
     * 코스 추천
     * - 항상 Gemini를 호출해서 코스를 생성
     * - CoursePresetProvider는 예시 few-shot 데이터로만 사용
     * - Gemini 실패 시에만 dummy 코스로 fallback
     */
    public CourseSummaryRes recommendCourses(RecommendCoursesReq req) {
        TravelRequest tr = travelRequestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new IllegalArgumentException("request not found"));

        // anchor / region 결정
        String anchor = (req.getAnchorId() != null && !req.getAnchorId().isBlank())
                ? req.getAnchorId()
                : tr.getAnchorId();

        if (anchor == null) {
            anchor = "guri"; // 안전 디폴트
        }

        String region = (tr.getRegionName() != null) ? tr.getRegionName() : "구리시";

        // ✅ 1) 프리셋을 "예시 JSON"으로 만들어 few-shot 컨텍스트에 넣기
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

        // ✅ 2) Gemini 프롬프트: 예시 + 사용자 조건 + 출력 스키마
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
                region, anchor,
                exampleSection,
                region,
                tr.getTravelDays(), tr.getBudget(),
                tr.getGender(), tr.getCompanions(),
                tr.getStyle(), tr.getAdditionalRequest()
        );

        String rawJson = null;
        try {
            rawJson = geminiClient.generate(props.getModelCourses(), prompt).block();
        } catch (Exception e) {
            System.out.println("[RecommendationService] ❗ Gemini API 호출 중 예외: " + e.getMessage());
        }

        // ✅ 3) Gemini 응답 파싱
        if (rawJson != null && !rawJson.isBlank()) {
            String json = extractJson(rawJson);
            if (!json.isBlank()) {
                try {
                    JsonNode root = mapper.readTree(json);
                    JsonNode nodes = root.path("courses");
                    List<CourseSummaryRes.CourseCard> list = new ArrayList<>();

                    if (nodes.isArray() && nodes.size() > 0) {
                        nodes.forEach(n -> {
                            List<CourseSummaryRes.PlaceBrief> places = new ArrayList<>();
                            n.path("places").forEach(p -> {
                                places.add(
                                        CourseSummaryRes.PlaceBrief.builder()
                                                .name(p.path("name").asText())
                                                .category(p.path("category").asText())
                                                .lat(p.path("lat").asDouble())
                                                .lng(p.path("lng").asDouble())
                                                .build()
                                );
                            });

                            List<String> tags = new ArrayList<>();
                            if (n.path("tags").isArray()) {
                                n.path("tags").forEach(t -> tags.add(t.asText()));
                            }

                            list.add(
                                    CourseSummaryRes.CourseCard.builder()
                                            .title(n.path("title").asText())
                                            .description(n.path("description").asText())
                                            .places(places)
                                            .totalDistance(n.path("totalDistance").asText())
                                            .estimatedTime(n.path("estimatedTime").asText())
                                            .tags(tags)
                                            .localCurrencyMerchants(n.path("localCurrencyMerchants").asInt(0))
                                            .build()
                            );
                        });

                        return CourseSummaryRes.builder()
                                .region(region)
                                .courses(list)
                                .build();
                    }
                } catch (Exception e) {
                    System.out.println("[RecommendationService] ⚠️ 코스 파싱 실패 → 더미 사용: " + e.getMessage());
                }
            }
        }

        // ✅ 4) 여기까지 왔으면 진짜로 실패한 경우 → fallback 더미 (예전과 동일)
        return CourseSummaryRes.builder()
                .region(region)
                .courses(List.of(
                        dummyCourse("감성 힐링 코스 (Fallback)"),
                        dummyCourse("자연&카페 원데이 (Fallback)")
                ))
                .build();
    }

    /** 코스 저장 */
    public CourseDetailRes saveCourse(SaveCourseReq req) {
        TravelRequest tr = travelRequestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new IllegalArgumentException("request not found"));

        Course course = Course.builder()
                .requestId(tr.getId())
                .anchorId(req.getAnchorId())
                .title(req.getTitle())
                .description(req.getDescription())
                .build();

        List<double[]> path = new ArrayList<>();

        req.getPlaces().stream()
                .sorted(Comparator.comparingInt(SaveCourseReq.PlaceItem::getOrderNo))
                .forEach(p -> {
                    CourseSegment seg = CourseSegment.builder()
                            .course(course)
                            .orderNo(p.getOrderNo())
                            .placeId(p.getPlaceId())
                            .placeName(p.getName())
                            .category(p.getCategory())
                            .lat(p.getLat())
                            .lng(p.getLng())
                            .transportMode(p.getTransportMode())
                            .build();
                    course.getSegments().add(seg);

                    if (p.getLat() != null && p.getLng() != null) {
                        path.add(new double[]{p.getLat(), p.getLng()});
                    }
                });

        double distanceKm = distanceClient.calculateTotalDistanceKm(path);
        int minutes = distanceClient.estimateMinutes(distanceKm);

        course.setTotalDistanceKm(distanceKm);
        course.setEstimatedMinutes(minutes);

        Course saved = courseRepository.save(course);

        return CourseDetailRes.builder()
                .courseId(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .region(tr.getRegionName())
                .totalDistanceKm(saved.getTotalDistanceKm())
                .estimatedMinutes(saved.getEstimatedMinutes())
                .segments(saved.getSegments().stream()
                        .map(s -> CourseDetailRes.Segment.builder()
                                .orderNo(s.getOrderNo())
                                .placeId(s.getPlaceId())
                                .placeName(s.getPlaceName())
                                .category(s.getCategory())
                                .lat(s.getLat())
                                .lng(s.getLng())
                                .transportMode(s.getTransportMode())
                                .build())
                        .toList())
                .build();
    }

    /** LLM 실패용 더미 */
    private CourseSummaryRes.CourseCard dummyCourse(String title) {
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
                .tags(List.of("자연", "카페"))
                .localCurrencyMerchants(12)
                .build();
    }
}
