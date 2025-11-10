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

    // 새로 추가된 프리셋
    private final RegionPresetProvider regionPresetProvider;
    private final CoursePresetProvider coursePresetProvider;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Gemini 응답 텍스트에서 순수한 JSON 블록만 추출
     */
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

    /**
     * 여행 요청 생성
     */
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

    /**
     * 지역 우선 추천
     * - Gemini가 뭘 줘도 우리는 의정부/구리/양주/동두천 중 하나로 스냅시킨다.
     */
    public RegionRecommendRes recommendRegion(Long requestId) {
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
            rawJson = geminiClient.generate(props.getModelRegion(), prompt).block();
        } catch (Exception e) {
            System.out.println("[RecommendationService] ❗ Gemini API 호출 중 예외: " + e.getMessage());
        }

        RegionRecommendRes picked = null;

        // 1) LLM이 뭔가 줬으면 → 우리가 지원하는 지역으로 스냅
        if (rawJson != null && !rawJson.isBlank()) {
            String json = extractJson(rawJson);
            if (!json.isBlank()) {
                try {
                    JsonNode node = mapper.readTree(json);
                    String region = node.path("region").asText();
                    String anchorId = node.path("anchorId").asText();
                    // 여기가 핵심: 우리 서비스에서 지원하는 anchor로 강제
                    picked = regionPresetProvider.snapToSupported(
                            (anchorId != null && !anchorId.isBlank()) ? anchorId : region
                    );
                } catch (Exception e) {
                    System.out.println("[RecommendationService] ⚠️ Gemini 응답 파싱 실패 -> 프리셋으로 전환: " + e.getMessage());
                }
            }
        }

        // 2) LLM이 비정상이거나 파싱 실패 → 프리셋 기본값
        if (picked == null) {
            picked = regionPresetProvider.snapToSupported(null); // 기본(구리) 같은 것
        }

        // 3) 요청에도 저장
        tr.setRegionName(picked.getRegion());
        tr.setAnchorId(picked.getAnchorId());
        travelRequestRepository.save(tr);

        return picked;
    }

    /**
     * 코스 추천
     * 1순위: anchorId에 해당하는 프리셋 코스 있으면 그거 바로 리턴
     * 2순위: 프리셋 없으면 Gemini 호출해서 파싱
     * 3순위: 그래도 실패하면 기존 더미
     */
    public CourseSummaryRes recommendCourses(RecommendCoursesReq req) {
        TravelRequest tr = travelRequestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new IllegalArgumentException("request not found"));

        // anchor / region 결정
        String anchor = (req.getAnchorId() != null && !req.getAnchorId().isBlank())
                ? req.getAnchorId()
                : tr.getAnchorId();

        if (anchor == null) {
            // 혹시 여행요청에 anchor를 못박아두지 못한 경우 안전장치
            anchor = "guri";
        }

        String region = (tr.getRegionName() != null) ? tr.getRegionName() : "구리시";

        // 1) 프리셋 먼저 조회
        var presetCourses = coursePresetProvider.findByAnchor(anchor);
        if (!presetCourses.isEmpty()) {
            return CourseSummaryRes.builder()
                    .region(region)
                    .courses(presetCourses)
                    .build();
        }

        // 2) 프리셋이 없으면 Gemini로 생성
        String prompt = """
                너는 여행 코스 설계자야. 지역: %s(앵커: %s)
                아래 사용자의 프로필을 보고 3개 이상의 코스 후보를 생성해.
                반드시 아래 스키마의 JSON으로만 반환해.

                {
                  "region": "%s",
                  "courses": [
                    {
                      "title": "...",
                      "description": "...",
                      "places": [
                        {"name":"...", "category":"...", "lat":37.5, "lng":127.3}
                      ],
                      "totalDistance": "18km",
                      "estimatedTime": "5시간",
                      "tags": ["자연","카페"],
                      "localCurrencyMerchants": 10
                    }
                  ]
                }

                사용자 입력:
                - 기간: %d일
                - 예산: %d원
                - 성별: %s
                - 동행: %s
                - 스타일: %s
                - 추가요청: %s
                """.formatted(
                region, anchor, region,
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

        // 3) 여기까지 왔으면 진짜로 아무 것도 못 받은 경우 → 더미
        return CourseSummaryRes.builder()
                .region(region)
                .courses(List.of(
                        dummyCourse("감성 힐링 코스 (Fallback)"),
                        dummyCourse("자연&카페 원데이 (Fallback)")
                ))
                .build();
    }

    /**
     * 추천 코스 확정/저장
     */
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

    /**
     * LLM 실패용 더미
     */
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
