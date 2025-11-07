package com.bukload.ai.web;

import com.bukload.ai.common.ApiResponse;
import com.bukload.ai.dto.request.SaveCourseReq;
import com.bukload.ai.dto.response.CourseDetailRes;
import com.bukload.ai.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/courses")
public class CourseController {

    private final RecommendationService service;

    /** 추천 코스 확정(저장) (POST /courses) */
    @PostMapping
    public ApiResponse<CourseDetailRes> save(@Valid @RequestBody SaveCourseReq req){
        return ApiResponse.ok(service.saveCourse(req));
    }
}
