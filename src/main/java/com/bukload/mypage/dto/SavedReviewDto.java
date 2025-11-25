package com.bukload.mypage.dto;

import com.bukload.review.domain.Review;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavedReviewDto {

    private Long reviewId;
    private String placeName;     // 장소명
    private String courseTitle;   // 코스명
    private String region;        // 지역명 (Course.anchorId)
    private String travelDays;  // 소요 시간 (Course.durationType)
    private String content;        // 후기 본문
    private String photoUrl;      // 후기 사진


    /**
     * ✅ 엔티티 → DTO 변환
     * Review → CourseSegment → (Place, Course)
     */
    public static SavedReviewDto from(Review review) {

        return SavedReviewDto.builder()
                .reviewId(review.getId())
                .placeName(review.getPlaceName())
                .courseTitle(review.getCourseTitle())
                .region(review.getRegion())
                .travelDays(review.getTravelDays())
                .content(review.getContent())
                .photoUrl(review.getS3Key())
                .build();
    }

}