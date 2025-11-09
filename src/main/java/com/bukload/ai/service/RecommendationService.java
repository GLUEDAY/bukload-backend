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
    public RegionRecommendRes recommendRegion(Long requestId){
        TravelRequest tr = travelRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("request not found"));

        String prompt = """
                너는 한국 여행 코스 추천 전문가야.
                아래 사용자의 여행 조건을 보고 '경기도 또는 인근' 기준으로 가장 적합한 '기초지자체(시/군)' 1곳을 추천해.
                반드시 JSON으로만 답해.
                필드: region(문자열, 예: 양평군), anchorId(영문 슬러그, 예: yangpyeong), comment(한 줄 코멘트), tags(문자열 배열)
                예) {"region":"양평군","anchorId":"yangpyeong","comment":"...","tags":["자연","카페","감성"]}

                입력:
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
            // 1) LLM 호출 및 블로킹 (API 오류 시 예외 발생)
            rawJson = geminiClient.generate(props.getModelRegion(), prompt).block();
        } catch (Exception e) {
            System.out.println("[RecommendationService] ❗ Gemini API 호출 중 예외 발생: " + e.getMessage());
            // 오류 발생 시 null로 처리하여 Fallback
        }

        // 2) 순수한 JSON만 추출 후 파싱 시도
        if (rawJson != null && !rawJson.isBlank()) {
            String json = extractJson(rawJson);
            if (!json.isBlank()) {
                try {
                    JsonNode node = mapper.readTree(json);
                    String region = node.path("region").asText();
                    String anchorId = node.path("anchorId").asText();
                    String comment = node.path("comment").asText("추천 지역입니다.");
                    List<String> tags = mapper.convertValue(node.path("tags"), List.class);

                    // 엔티티에도 저장
                    tr.setRegionName(region);
                    tr.setAnchorId(anchorId);
                    travelRequestRepository.save(tr); // 변경사항 저장

                    return RegionRecommendRes.builder()
                            .region(region)
                            .anchorId(anchorId)
                            .comment(comment)
                            .tags(tags)
                            .build();
                } catch (Exception e) {
                    System.out.println("[RecommendationService] ⚠️ Gemini 응답 파싱 실패 → fallback 사용: " + e.getMessage());
                }
            }
        }

        // 3) LLM이 비었거나, JSON 파싱 안 된 경우 → 더미 (Fallback)
        final String fallbackRegion = "양평군";
        final String fallbackAnchor = "yangpyeong";

        tr.setRegionName(fallbackRegion);
        tr.setAnchorId(fallbackAnchor);
        travelRequestRepository.save(tr); // 변경사항 저장

        return RegionRecommendRes.builder()
                .region(fallbackRegion)
                .anchorId(fallbackAnchor)
                .comment("감성 여행지 (Fallback)")
                .tags(List.of("자연","카페"))
                .build();
    }

    /** 코스 추천 */
    public CourseSummaryRes recommendCourses(RecommendCoursesReq req){
        TravelRequest tr = travelRequestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new IllegalArgumentException("request not found"));

        String region = tr.getRegionName() != null ? tr.getRegionName() : "양평군";
        String anchor = (req.getAnchorId()!=null)? req.getAnchorId()
                : (tr.getAnchorId()!=null? tr.getAnchorId() : "yangpyeong");

        String prompt = """
                너는 여행 코스 설계자야. 지역: %s(앵커: %s)
                아래 프로필을 보고 3개 이상의 코스 후보를 생성해.
                각 코스는 title, description, places(이름/카테고리/위경도), totalDistance, estimatedTime, tags, localCurrencyMerchants 로 JSON 배열로만 반환해.
                스키마 예시는 아래와 같다.
                {
                  "region": "%s",
                  "courses": [
                    {
                      "title": "...",
                      "description": "...",
                      "places": [{"name":"...", "category":"...", "lat":37.5, "lng":127.3}],
                      "totalDistance": "18km",
                      "estimatedTime": "5시간",
                      "tags": ["자연","카페"],
                      "localCurrencyMerchants": 10
                    }
                  ]
                }

                사용자 입력:
                - 기간: %d일, 예산: %d원, 성별: %s, 동행: %s, 스타일: %s, 추가요청: %s
                """.formatted(region, anchor, region,
                tr.getTravelDays(), tr.getBudget(), tr.getGender(), tr.getCompanions(),
                tr.getStyle(), tr.getAdditionalRequest());

        String rawJson = null;
        try {
            rawJson = geminiClient.generate(props.getModelCourses(), prompt).block();
        } catch (Exception e) {
            System.out.println("[RecommendationService] ❗ Gemini API 호출 중 예외 발생: " + e.getMessage());
        }

        if (rawJson != null && !rawJson.isBlank()) {
            String json = extractJson(rawJson);
            if (!json.isBlank()) {
                try {
                    JsonNode root = mapper.readTree(json);
                    JsonNode nodes = root.path("courses");
                    List<CourseSummaryRes.CourseCard> list = new ArrayList<>();

                    // nodes가 배열이 아니거나 비어있으면 건너뛰기
                    if(nodes.isArray() && nodes.size() > 0) {
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

                            list.add(
                                    CourseSummaryRes.CourseCard.builder()
                                            .title(n.path("title").asText())
                                            .description(n.path("description").asText())
                                            .places(places)
                                            .totalDistance(n.path("totalDistance").asText())
                                            .estimatedTime(n.path("estimatedTime").asText())
                                            .tags(mapper.convertValue(n.path("tags"), List.class))
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

        // 실패 시 더미 (Fallback)
        return CourseSummaryRes.builder()
                .region(region)
                .courses(List.of(dummyCourse("감성 힐링 코스 (Fallback)"), dummyCourse("자연&카페 원데이 (Fallback)")))
                .build();
    }

    public CourseDetailRes saveCourse(SaveCourseReq req){
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
                    if (p.getLat()!=null && p.getLng()!=null) {
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
                .segments(saved.getSegments().stream().map(s ->
                        CourseDetailRes.Segment.builder()
                                .orderNo(s.getOrderNo())
                                .placeId(s.getPlaceId())
                                .placeName(s.getPlaceName())
                                .category(s.getCategory())
                                .lat(s.getLat())
                                .lng(s.getLng())
                                .transportMode(s.getTransportMode())
                                .build()).toList())
                .build();
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
}