package com.bukload.ai.dto.dto;

import com.bukload.ai.domain.course.CourseSegment;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSegmentForCourseDetail {

    private Long courseSegmentId;     // ⭐ 추가됨

    private String placeId;
    private String placeName;
    private Boolean localpayOX;
    private String transitJson;

    private Integer orderNo;
    private Double lat;
    private Double lng;

    public static CourseSegmentForCourseDetail from(CourseSegment seg) {
        return CourseSegmentForCourseDetail.builder()
                .courseSegmentId(seg.getId())       // ⭐ ID 매핑
                .placeId(seg.getPlaceId())
                .placeName(seg.getPlaceName())
                .localpayOX(seg.getLocalpayOX())
                .transitJson(seg.getTransitJson())
                .orderNo(seg.getOrderNo())
                .lat(seg.getLat())
                .lng(seg.getLng())
                .build();
    }
}
