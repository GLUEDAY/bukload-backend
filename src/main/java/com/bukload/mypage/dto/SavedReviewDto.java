package com.bukload.mypage.dto;

import com.bukload.ai.domain.review.Review;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavedReviewDto {

    private String placeName;     // 장소명
    private String courseTitle;   // 코스명
    private String region;        // 지역명 (Course.anchorId)
    private String durationType;  // 소요 시간 (Course.durationType)
    private String photoUrl;      // 후기 사진
    private String text;          // 후기 본문

    /**
     * ✅ 엔티티 → DTO 변환
     * Review → CourseSegment → (Place, Course)
     */
    public static SavedReviewDto from(Review review) {
        return SavedReviewDto.builder()
                .placeName(review.getSegment().getPlace().getName())
                .courseTitle(review.getSegment().getCourse().getTitle())
                .region(review.getSegment().getCourse().getAnchorId())       // ✅ 지역 추가
                .durationType(review.getSegment().getCourse().getDurationType()) // ✅ 시간 정보 추가
                .photoUrl(review.getPhotoUrl())
                .text(review.getText())
                .build();
    }
}
