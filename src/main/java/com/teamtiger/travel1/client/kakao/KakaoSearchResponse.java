package com.teamtiger.travel1.client.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoSearchResponse {
    private List<Document> documents;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        private String id;
        private String place_name;
        private String category_group_code;
        private String category_group_name;
        private String road_address_name;
        private String address_name;
        private String x; // lng
        private String y; // lat
        private String place_url; // 카카오 지도 링크
    }
}
