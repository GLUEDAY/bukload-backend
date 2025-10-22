package com.teamtiger.travel1.controller;

import com.teamtiger.travel1.dto.PlaceResponse;
import com.teamtiger.travel1.service.PlaceSearchService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceSearchService placeSearchService;

    /**
     * 예: GET /api/places/search?query=의정부역
     */
    @GetMapping("/search")
    public ResponseEntity<PlaceResponse> search(@RequestParam @NotBlank String query) {
        PlaceResponse result = placeSearchService.searchOne(query);
        return ResponseEntity.ok(result);
    }
}
