// com/teamtiger/travel1/client/google/GoogleDetailsResponse.java
package com.teamtiger.travel1.client.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleDetailsResponse {
    private String status;
    private String error_message;
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

        // ✅ 기본/현재 영업시간 모두 받을 수 있게 정의
        private OpeningHours opening_hours;
        private OpeningHours current_opening_hours;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpeningHours {
        private Boolean open_now;
        // "Monday: 9:00 AM – 6:00 PM" 같은 텍스트
        private List<String> weekday_text;
        // 디테일한 시간(HHmm, 요일 인덱스)
        private List<Period> periods;

        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Period {
            private TimeDetail open;  // day(0=Sun..6=Sat), time("0930")
            private TimeDetail close; // day(0=Sun..6=Sat), time("1830")
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TimeDetail {
            private Integer day;
            private String time; // "0930"
            private String date; // 있을 때만 옴(예외일)
        }
    }
}
