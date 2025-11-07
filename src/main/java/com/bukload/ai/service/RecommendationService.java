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

    public RegionRecommendRes recommendRegion(Long requestId){
        TravelRequest tr = travelRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("request not found"));

        String prompt = """
                너는 한국 여행 코스 추천 전문가야.
                아래 사용자의 여행 조건을 보고 '경기도 또는 인근' 기준으로 가장 적합한 '기초지자체(시/군)' 1곳을 추천해.
                반드시 JSON으로만 답해. 
                필드: region, anchorId, comment, tags

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

        String json = geminiClient.generate(props.getModelRegion(), prompt).block();
        if (json == null || json.isBlank()) {
            json = """
                   {"region":"양평군","anchorId":"yangpyeong","comment":"감성 여행지","tags":["자연","카페"]}
                   """;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            var parsed = mapper.readTree(json);

            String region = parsed.path("region").asText("양평군");
            String anchorId = parsed.path("anchorId").asText("yangpyeong");

            tr.setRegionName(region);
            tr.setAnchorId(anchorId);

            return RegionRecommendRes.builder()
                    .region(region)
                    .anchorId(anchorId)
                    .comment(parsed.path("comment").asText("추천 지역입니다."))
                    .tags(mapper.convertValue(parsed.path("tags"), List.class))
                    .build();

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // 파싱 실패하면 기본값
            tr.setRegionName("양평군");
            tr.setAnchorId("yangpyeong");
            return RegionRecommendRes.builder()
                    .region("양평군")
                    .anchorId("yangpyeong")
                    .comment("추천 지역입니다.")
                    .tags(List.of("자연","카페"))
                    .build();
        }
    }

    public CourseSummaryRes recommendCourses(RecommendCoursesReq req){
        TravelRequest tr = travelRequestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new IllegalArgumentException("request not found"));

        String region = tr.getRegionName() != null ? tr.getRegionName() : "양평군";
        String anchor = (req.getAnchorId()!=null)? req.getAnchorId()
                : (tr.getAnchorId()!=null? tr.getAnchorId() : "yangpyeong");

        String prompt = """
                너는 여행 코스 설계자야. 지역: %s(앵커: %s)
                3개 이상의 코스를 JSON 형식으로만 반환해.
                스키마:
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

        String json = geminiClient.generate(props.getModelCourses(), prompt).block();
        if (json == null || json.isBlank()) {
            return CourseSummaryRes.builder()
                    .region(region)
                    .courses(List.of(
                            dummyCourse("감성 힐링 코스"),
                            dummyCourse("자연&카페 원데이")
                    ))
                    .build();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            var root = mapper.readTree(json);
            var nodes = root.path("courses");
            var list = new ArrayList<CourseSummaryRes.CourseCard>();

            nodes.forEach(n -> {
                try {
                    var placesNode = n.path("places");
                    var places = new ArrayList<CourseSummaryRes.PlaceBrief>();
                    placesNode.forEach(p -> places.add(
                            CourseSummaryRes.PlaceBrief.builder()
                                    .name(p.path("name").asText())
                                    .category(p.path("category").asText())
                                    .lat(p.path("lat").asDouble())
                                    .lng(p.path("lng").asDouble())
                                    .build()
                    ));

                    list.add(CourseSummaryRes.CourseCard.builder()
                            .title(n.path("title").asText())
                            .description(n.path("description").asText())
                            .places(places)
                            .totalDistance(n.path("totalDistance").asText())
                            .estimatedTime(n.path("estimatedTime").asText())
                            .tags(mapper.convertValue(n.path("tags"), List.class))
                            .localCurrencyMerchants(n.path("localCurrencyMerchants").asInt(0))
                            .build());
                } catch (Exception ignore) { }
            });

            return CourseSummaryRes.builder()
                    .region(region)
                    .courses(list)
                    .build();

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return CourseSummaryRes.builder()
                    .region(region)
                    .courses(List.of(
                            dummyCourse("감성 힐링 코스"),
                            dummyCourse("자연&카페 원데이")
                    ))
                    .build();
        }
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
