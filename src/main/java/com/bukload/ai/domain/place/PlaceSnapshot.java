package com.bukload.ai.domain.place;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlaceSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalPlaceId;  // kakao/google id
    private String name;
    private String category;
    private Double lat;
    private Double lng;
    private String address;
    private Double rating;
    private Integer reviewCount;
    private String homepageUrl;
    private String mapUrl;
    private Boolean openNow;
}
