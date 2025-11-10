package com.bukload.ai.service.preset;

import com.bukload.ai.dto.response.CourseSummaryRes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CoursePresetProvider {

    // anchorId -> 코스 리스트
    private static final Map<String, List<CourseSummaryRes.CourseCard>> PRESET = Map.of(
            "uijeongbu", List.of(
                    CourseSummaryRes.CourseCard.builder()
                            .title("의정부 미술도서관 & 감성 카페")
                            .description("문화·전시 감상 후 근처 감성 카페로 이동하는 코스")
                            .places(List.of(
                                    CourseSummaryRes.PlaceBrief.builder().name("의정부 미술도서관").category("문화시설").lat(37.74).lng(127.04).build(),
                                    CourseSummaryRes.PlaceBrief.builder().name("의정부 감성 카페").category("카페").lat(37.741).lng(127.042).build()
                            ))
                            .totalDistance("4km")
                            .estimatedTime("3시간")
                            .tags(List.of("문화", "전시", "카페"))
                            .localCurrencyMerchants(4)
                            .build()
            ),
            "guri", List.of(
                    CourseSummaryRes.CourseCard.builder()
                            .title("구리 한강시민공원 산책 코스")
                            .description("한강뷰 산책 후 카페로 이동하는 가벼운 반나절 코스")
                            .places(List.of(
                                    CourseSummaryRes.PlaceBrief.builder().name("구리 한강시민공원").category("공원").lat(37.593).lng(127.140).build(),
                                    CourseSummaryRes.PlaceBrief.builder().name("구리 카페거리").category("카페").lat(37.595).lng(127.147).build()
                            ))
                            .totalDistance("5km")
                            .estimatedTime("3시간")
                            .tags(List.of("산책", "카페"))
                            .localCurrencyMerchants(5)
                            .build()
            ),
            "yangju", List.of(
                    CourseSummaryRes.CourseCard.builder()
                            .title("양주 감성 카페 & 나리공원")
                            .description("사진 찍기 좋은 감성 스팟 위주 코스")
                            .places(List.of(
                                    CourseSummaryRes.PlaceBrief.builder().name("나리공원").category("공원").lat(37.774).lng(127.045).build(),
                                    CourseSummaryRes.PlaceBrief.builder().name("양주 감성카페").category("카페").lat(37.77).lng(127.05).build()
                            ))
                            .totalDistance("7km")
                            .estimatedTime("4시간")
                            .tags(List.of("감성", "사진", "카페"))
                            .localCurrencyMerchants(3)
                            .build()
            ),
            "dongducheon", List.of(
                    CourseSummaryRes.CourseCard.builder()
                            .title("동두천 중앙공원 소소한 여행")
                            .description("북부 쪽으로 조용하게 다녀오는 당일치기")
                            .places(List.of(
                                    CourseSummaryRes.PlaceBrief.builder().name("동두천 중앙공원").category("공원").lat(37.9).lng(127.06).build(),
                                    CourseSummaryRes.PlaceBrief.builder().name("동두천 로컬 맛집").category("식당").lat(37.91).lng(127.058).build()
                            ))
                            .totalDistance("4km")
                            .estimatedTime("3시간")
                            .tags(List.of("조용한", "로컬"))
                            .localCurrencyMerchants(2)
                            .build()
            )
    );

    public List<CourseSummaryRes.CourseCard> findByAnchor(String anchorId) {
        return PRESET.getOrDefault(anchorId, List.of());
    }
}
