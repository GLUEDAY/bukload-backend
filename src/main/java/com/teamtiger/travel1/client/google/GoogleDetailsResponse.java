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
        private String url;
        private String website;
        private String formatted_phone_number;

        // ✅ 사진 메타데이터
        private List<Photo> photos;

        // 영업시간
        private OpeningHours opening_hours;
        private OpeningHours current_opening_hours;
    }

    // ✅ Photo 매핑
    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo {
        private Integer width;
        private Integer height;
        private String photo_reference;
        private List<String> html_attributions;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpeningHours {
        private Boolean open_now;
        private List<String> weekday_text;
        private List<Period> periods;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Period {
            private TimeDetail open;
            private TimeDetail close;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TimeDetail {
            private Integer day;
            private String time;
            private String date;
        }
    }
}
