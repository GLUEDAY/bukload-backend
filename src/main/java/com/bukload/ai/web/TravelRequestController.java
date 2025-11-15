package com.bukload.ai.web;

import com.bukload.ai.common.ApiResponse;
import com.bukload.ai.dto.request.CreateTravelRequestReq;
import com.bukload.ai.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/travel-requests")
public class TravelRequestController {

    private final RecommendationService service;

    /** 여행 요청 생성 (POST /travel-requests) */
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody CreateTravelRequestReq req){
        Long id = service.createTravelRequest(req);
        return ApiResponse.ok(id);
    }
}
