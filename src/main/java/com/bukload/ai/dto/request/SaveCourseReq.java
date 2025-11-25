package com.bukload.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SaveCourseReq {
    @NotNull
    private Long requestId;
    @NotBlank
    private String anchorId;
    @NotBlank
    private String title;
    private String description;

    @NotNull
    private List<PlaceItem> places;

    @Data
    public static class PlaceItem {
        private String placeId;
        private String name;
        private String category;
        private Double lat;
        private Double lng;
        private Integer orderNo;
        private String transportMode;
    }
}
