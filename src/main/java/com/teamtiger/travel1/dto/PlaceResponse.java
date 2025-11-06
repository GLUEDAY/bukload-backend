package com.teamtiger.travel1.dto;

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

    // ===== Google 보강 필드 =====
    private Float rating;
    private Integer reviewCount;
    private String homepageUrl;
    private String mapUrl;
    private Boolean openNow;

    // ✅ 추가: 요일별 영업시간(“Monday: 9:00 AM – 6:00 PM” 형식)
    private List<String> openingHoursText;
}