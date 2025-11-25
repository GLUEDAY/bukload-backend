// com/teamtiger/travel1/dto/PlaceResponse.java
package com.bukload.travel1.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor @Builder
public class PlaceResponse {
    private String placeId;
    private String name;
    private String category;
    private double lat;
    private double lng;
    private String address;
    private String phone;

    // ===== Google 보강 필드 =====
    private Float rating;
    private Integer reviewCount;
    private String homepageUrl;
    private String mapUrl;
    private Boolean openNow;
    private List<String> openingHoursText;

    // ✅ 대표사진(프록시 URL). 프론트는 이 URL만 <img src=...>로 쓰면 됨.
    private String representativePhotoUrl;
}
