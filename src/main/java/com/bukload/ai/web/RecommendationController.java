package com.bukload.ai.web;

import com.bukload.ai.common.ApiResponse;
import com.bukload.ai.dto.request.RecommendCoursesReq;
import com.bukload.ai.dto.response.CourseSummaryRes;
import com.bukload.ai.dto.response.RegionRecommendRes;
import com.bukload.ai.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService service;

    @GetMapping("/health")
    public String health(){ return "AI Recommendation Service OK"; }

    /** AI 지역 우선 추천 (POST /recommendations/region) */
    @PostMapping("/region")
    public ApiResponse<RegionRecommendRes> region(@RequestParam Long requestId){
        return ApiResponse.ok(service.recommendRegion(requestId));
    }

    /** AI 코스 추천 (POST /recommendations/courses) */
    @PostMapping("/courses")
    public ApiResponse<CourseSummaryRes> courses(@Valid @RequestBody RecommendCoursesReq req){
        return ApiResponse.ok(service.recommendCourses(req));
    }
}
