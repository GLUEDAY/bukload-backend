package com.bukload.ai.dto.response;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseDetailRes {
    private Long courseId;
    private String title;
    private String description;
    private String region;
    private Double totalDistanceKm;
    private Integer estimatedMinutes;
    private List<Segment> segments;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Segment {
        private Integer orderNo;
        private String placeId;
        private String placeName;
        private String category;
        private Double lat;
        private Double lng;
        private String transportMode;
    }
}
