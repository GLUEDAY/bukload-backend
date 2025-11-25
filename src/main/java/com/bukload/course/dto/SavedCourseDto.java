package com.bukload.course.dto;

import com.bukload.ai.domain.course.Course;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavedCourseDto {

    private Long courseId;     // 코스 ID (상세 이동용)
    private String title;      // 코스 제목
    private String region;     // 지역(anchorId)
    private String imageUrl;   // 대표 이미지
    private String travelDays; // 예상 소요 기간 문자열 (ex: 하루, 1박2일)

    /**
     * 엔터티 -> DTO 변환 메서드
     */
    public static SavedCourseDto from(Course course) {
        return SavedCourseDto.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .region(course.getAnchorId())
                .imageUrl(course.getImageUrl())
                .travelDays(course.getTravelDays())
                .build();
    }

}
