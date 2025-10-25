package com.teamtiger.travel1.dto;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor @Builder
public class PlaceResponse {
    private String placeId;      // 우선: 카카오 id (fallback). 구글 place_id일 수 있음
    private String name;
    private String category;     // CAFE/FOOD/STAY/LAND MARK 등
    private double lat;
    private double lng;
    private String address;

    // ===== 구글 보강 전용 필드 =====
    private Float rating;         // Google rating
    private Integer reviewCount;  // Google user_ratings_total
    private String homepageUrl;   // Google website
    private String mapUrl;        // Google url or Kakao place_url

    // 선택: 현재 영업중 여부(구글 only). 필요 없으면 제거 가능
    private Boolean openNow;
}
