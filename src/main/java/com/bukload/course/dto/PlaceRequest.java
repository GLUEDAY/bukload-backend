package com.bukload.course.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceRequest {
    private String name;
    private String address;
    private String category;
    private Double lat;
    private Double lng;
    private String description;
}
