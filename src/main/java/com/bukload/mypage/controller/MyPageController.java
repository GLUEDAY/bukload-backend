package com.bukload.mypage.controller;

import com.bukload.auth.user.User;
import com.bukload.course.dto.SavedCourseDto;
import com.bukload.mypage.dto.MyPageResponse;
import com.bukload.mypage.dto.SavedReviewDto;
import com.bukload.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * ✅ 마이페이지 메인 조회 (포인트 + 저장된 코스)
     */
    @GetMapping("/me/mypage")
    public ResponseEntity<MyPageResponse> getMyPage(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(myPageService.getMyPage(user));
    }

    /**
     * ✅ 사용자가 저장한 코스 목록 조회
     * - 포인트 제외, 코스 요약 정보만 반환
     */
    @GetMapping("/me/saved-courses")
    public ResponseEntity<List<SavedCourseDto>> getSavedCourses(@AuthenticationPrincipal User user) {
        List<SavedCourseDto> savedCourses = myPageService.getSavedCourses(user);
        return ResponseEntity.ok(savedCourses);
    }

    /**
     * ✅ 사용자가 작성한 후기 목록 조회
     * - 장소명, 코스명, 사진, 텍스트 포함
     */
    @GetMapping("/me/saved-reviews")
    public ResponseEntity<List<SavedReviewDto>> getSavedReviews(@AuthenticationPrincipal User user) {
        List<SavedReviewDto> savedReviews = myPageService.getSavedReviews(user);
        return ResponseEntity.ok(savedReviews);
    }
}