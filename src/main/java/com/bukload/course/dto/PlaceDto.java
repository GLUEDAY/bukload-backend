package com.bukload.course.dto;

import com.bukload.travel1.dto.PlaceResponse;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceDto {
    private Long id;
    private String placeId;
    private String name;
    private String category;
    private Double lat;
    private Double lng;
    private String address;
    private Boolean localpayOX;


    public static PlaceDto fromSearchResult(PlaceResponse r) {
        return PlaceDto.builder()
                .placeId(r.getPlaceId())     // ⭐ 중요
                .name(r.getName())
                .category(r.getCategory())
                .lat(r.getLat())
                .lng(r.getLng())
                .address(r.getAddress())
                .build();
    }


}
