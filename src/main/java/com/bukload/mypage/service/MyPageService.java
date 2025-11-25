package com.bukload.mypage.service;

import com.bukload.auth.user.User;
import com.bukload.ai.domain.course.Course;
import com.bukload.ai.domain.course.CourseRepository;
import com.bukload.review.domain.Review;
import com.bukload.review.domain.ReviewRepository;
import com.bukload.mypage.dto.MyPageResponse;
import com.bukload.course.dto.SavedCourseDto;
import com.bukload.mypage.dto.SavedReviewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final CourseRepository courseRepository;
    private final ReviewRepository reviewRepository;

    /**
     * ✅ 마이페이지 메인 조회 (포인트 + 저장된 코스)
     */
    public MyPageResponse getMyPage(User user) {
        Long userId = user.getId();
        int userPoint = user.getPoint();

        List<Course> courses = courseRepository.findByUserId(userId);

        List<SavedCourseDto> savedCourses = courses.stream()
                .map(SavedCourseDto::from)
                .collect(Collectors.toList());

        return MyPageResponse.builder()
                .userPoint(userPoint)
                .savedCourses(savedCourses)
                .build();
    }

    /**
     * ✅ 저장된 코스 목록만 조회
     */
    public List<SavedCourseDto> getSavedCourses(User user) {
        Long userId = user.getId();

        List<Course> courses = courseRepository.findByUserId(userId);

        return courses.stream()
                .map(SavedCourseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 사용자가 작성한 후기 목록 조회
     */
    public List<SavedReviewDto> getSavedReviews(User user) {
        Long userId = user.getId();

        List<Review> reviews = reviewRepository.findByUserId(userId);

        return reviews.stream()
                .map(SavedReviewDto::from)
                .collect(Collectors.toList());
    }
}