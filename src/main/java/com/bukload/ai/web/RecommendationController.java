package com.bukload.ai.web;

import com.bukload.ai.common.ApiResponse;
import com.bukload.ai.dto.request.RecommendCoursesReq;
import com.bukload.ai.dto.response.CourseSummaryRes;
import com.bukload.ai.dto.response.RegionRecommendRes;
import com.bukload.ai.dto.response.SavedCourseDtoForTemp;
import com.bukload.ai.service.RecommendationService;
import com.bukload.auth.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService service;

    @GetMapping("/health")
    public String health(){ return "AI Recommendation Service OK"; }

    /** AI 지역 우선 추천 (POST /recommendations/region) */
    @PostMapping("/region")
    public ApiResponse<RegionRecommendRes> region(@RequestParam Long requestId, @AuthenticationPrincipal User user){
        return ApiResponse.ok(service.recommendRegion(requestId, user));
    }

    /** AI 코스 추천 (POST /recommendations/courses)
     * 프론트에서 req넣어주면 그걸로 recommendCourses호출하고 그게 travelRequestRepository에서 요청서를 찾아옴
     *
     */

    @PostMapping("/courses")
    public ApiResponse<List<SavedCourseDtoForTemp>> courses(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RecommendCoursesReq req) {

        return ApiResponse.ok(service.recommendCourses(req, user));
    }



}
