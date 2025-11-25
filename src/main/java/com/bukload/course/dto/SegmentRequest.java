package com.bukload.course.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SegmentRequest {
    private Long courseId;
    private String placeName;   // placeId 대신 placeName
}

