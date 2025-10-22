package com.teamtiger.travel1.dto;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor @Builder
public class PlaceResponse {
    private String placeId;
    private String name;
    private String category;      // ENUM 매핑된 문자열: CAFE/FOOD/STAY/LAND MARK 등
    private double lat;
    private double lng;
    private String address;
    private Float rating;         // 구글에서만 제공(평균 평점)
    private Integer reviewCount;  // 구글에서만 제공(리뷰 수)
    private String homepageUrl;   // ✅ 공식 홈페이지(google: website)
    private String mapUrl;        // ✅ 지도 페이지(google: url, kakao: place_url)
}
