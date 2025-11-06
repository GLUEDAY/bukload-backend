package com.teamtiger.travel1.client.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleTextSearchResponse {
    private String status;          // OK, ZERO_RESULTS, REQUEST_DENIED 등
    private String error_message;   // 에러 메시지
    private List<Result> results;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String place_id;
        private String name;
        private Geometry geometry;
        private String formatted_address;
        private List<String> types;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        private Location location;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double lat;
        private double lng;
    }
}
