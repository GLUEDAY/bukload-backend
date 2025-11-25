package com.bukload.ai.web;

import com.bukload.ai.common.ApiResponse;
import com.bukload.ai.service.RecommendationService;
import com.bukload.auth.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/courses")
public class AiCourseController {

    private final RecommendationService service;

    /** 추천 코스 확정(저장) (POST /courses) */
    @PostMapping
    public ApiResponse<Long> save(@AuthenticationPrincipal User user, @RequestParam("tempCourseId") Long tempCourseId) {
        Long savedId = service.saveCourse(tempCourseId, user);
        return ApiResponse.ok(savedId);
    }
}
