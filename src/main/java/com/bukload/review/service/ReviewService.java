package com.bukload.review.service;

import com.bukload.review.domain.Review;
import com.bukload.review.domain.ReviewRepository;
import com.bukload.ai.domain.course.CourseSegment;
import com.bukload.ai.domain.course.CourseSegmentRepository;
import com.bukload.auth.user.User;
import com.bukload.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CourseSegmentRepository courseSegmentRepository;

    /**
     * 리뷰 저장 메서드
     */
    public Review saveReview(
            Long userId,
            Long courseSegmentId,
            String content,
            String s3Key,
            String contentType,
            Long sizeBytes
    ) {
        // --- 유저 조회 ---
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. userId=" + userId));

        // --- 코스 세그먼트 조회 ---
        CourseSegment segment = courseSegmentRepository.findById(courseSegmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 코스 세그먼트를 찾을 수 없습니다. courseSegmentId=" + courseSegmentId));

        // 🌟 후기 작성 보상 포인트 지급
        int rewardPoint = 100;
        user.addPoint(rewardPoint);
        // (Dirty Checking으로 자동 저장)

        // --- Review 엔티티 생성 ---
        Review review = Review.builder()
                .user(user)
                .courseSegment(segment)

                // === 스냅샷 필드 ===
                .placeName(segment.getPlaceName())
                .courseTitle(segment.getCourse().getTitle())
                .region(segment.getCourse().getRegionName())
                .travelDays(segment.getCourse().getTravelDays())

                // === 파일 및 내용 ===
                .content(content)
                .s3Key(s3Key)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .build();

        // --- 저장 ---
        return reviewRepository.save(review);
    }
}
