package com.teamtiger.travel1.client.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleDetailsResponse {
    private String status;
    private String error_message;   // 에러 메시지(거절 사유 등)
    private Detail result;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Detail {
        private String place_id;
        private String name;
        private GoogleTextSearchResponse.Geometry geometry;
        private String formatted_address;
        private List<String> types;
        private Double rating;
        private Integer user_ratings_total;
        private String url;      // Google Maps URL
        private String website;  // 공식 홈페이지
        private OpeningHours current_opening_hours;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpeningHours {
        private Boolean open_now;
    }
}
