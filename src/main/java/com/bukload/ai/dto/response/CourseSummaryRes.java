package com.bukload.ai.dto.response;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseSummaryRes {
    private String region;
    private List<CourseCard> courses;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CourseCard {
        private String title;
        private String description;
        private List<PlaceBrief> places;
        private String totalDistance;   // "18km"
        private String estimatedTime;   // "5시간"
        private List<String> tags;      // ["자연","카페"]
        private Integer localCurrencyMerchants; // 지역화폐 가맹점 수(간단 stub)
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlaceBrief {
        private String name;
        private String category;
        private Double lat;
        private Double lng;
    }
}
