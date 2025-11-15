package com.bukload.course.dto;

import com.bukload.ai.domain.course.Course;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavedCourseDto {

    private String title;      // 코스 제목
    private String region;     // 지역 (anchorId)
    private String imageUrl;   // 대표 이미지
    private String travelDays; // 소요 시간 유형 (ex: 반나절, 하루)

    /**
     * ✅ 엔티티 → DTO 변환 메서드
     */
    public static SavedCourseDto from(Course course) {
        return SavedCourseDto.builder()
                .title(course.getTitle())
                .region(course.getAnchorId())
                .imageUrl(course.getImageUrl())
                .travelDays(course.getTravelDays())
                .build();
    }

}
