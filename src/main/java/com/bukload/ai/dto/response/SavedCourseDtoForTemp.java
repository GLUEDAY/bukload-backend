package com.bukload.ai.dto.response;

import com.bukload.ai.domain.course.TempCourse;
import com.bukload.ai.domain.request.TravelRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavedCourseDtoForTemp {

    private Long tempCourseId; // ⭐ tempCourseId 추가
    private String title;
    private String region;
    private String imageUrl;
    private String travelDays;

    /**
     * ✅ TempCourse + TravelRequest → DTO 변환
     */
    public static SavedCourseDtoForTemp from(TempCourse tempCourse, TravelRequest tr) {
        return SavedCourseDtoForTemp.builder()
                .tempCourseId(tempCourse.getId())     // ⭐ 추가
                .title(tempCourse.getTitle())
                .region(tempCourse.getAnchorId())
                .imageUrl(null)
                .travelDays(
                        tr.getTravelDays() != null
                                ? tr.getTravelDays() + "일"
                                : null
                )
                .build();
    }
}
