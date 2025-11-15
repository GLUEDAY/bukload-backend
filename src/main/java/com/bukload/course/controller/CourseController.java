package com.bukload.course.controller;

import com.bukload.course.dto.*;
import com.bukload.course.service.CourseService;
import com.bukload.course.service.PlaceService;
import com.bukload.auth.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final PlaceService placeService;

    /**
     * ✅ 내 코스 목록 조회 (GET /courses?mine=true)
     */
    @GetMapping
    public ResponseEntity<?> getCourses(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Boolean mine) {

        if (Boolean.TRUE.equals(mine)) {
            return ResponseEntity.ok(courseService.getMyCourses(user));
        }
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    /**
     * ✅ 코스 상세 조회 (GET /courses/{courseId})
     * 6p
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<?> getCourseDetail(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }


    /**
     * ✅ 장소 추가 직전 코스 수정사항 저장 (POST /courses/{courseId}/segments?order=1,2,3)
     * 장소 추가하기 전에 이전에 코스 수정하던 사항 저장
     * "장소 추가하기"로 넘어가는 api이지 실제로 장소를 선택해서 db에 저장하는 과정은 아님.
     * 8p
     */
    @PutMapping("/{courseId}/segments")
    public ResponseEntity<?> updateCourseSegments(
            @PathVariable Long courseId,
            @RequestParam List<Long> order
    ) {
        courseService.updateSegmentOrderAndDelete(courseId, order);
        return ResponseEntity.ok().build();
    }

    /**
     * ✅ 코스 수정하기 저장 버튼 눌렀을 때 – 순서/삭제 반영 + ""이동정보 갱신""
     * (PUT /courses/{courseId}/segments/fullupdate)
     */
    @PutMapping("/{courseId}/segments/fullupdate")
    public ResponseEntity<?> fullupdateCourseSegments(
            @PathVariable Long courseId,
            @RequestParam List<Long> order
    ) {
        // 1️⃣ 기존 기능: 순서 변경 + 삭제
        courseService.updateSegmentOrderAndDelete(courseId, order);

        // 2️⃣ 새로운 기능: 이동 정보만 재계산
        courseService.updateTransitJson(courseId);

        return ResponseEntity.ok().build();
    }


    /**
     * ✅ 새로운 장소를 실제 DB에 추가하는 API
     * (POST /courses/{courseId}/segments/db)
     * 29p
     */
    @PostMapping("/{courseId}/segments/db")
    public ResponseEntity<?> addSegmentDB(
            @PathVariable Long courseId,
            @RequestParam String placeName
    ) {
        courseService.addSegmentFromPopup(courseId, placeName);
        return ResponseEntity.ok().build();
    }




    /**
     * ✅ 장소 키워드 검색 (GET /courses/places/search)
     * - category 제거됨
     * 29p, 30p
     * 키워드 검색이랑 지도에서 찾기랑 같이 처리됨
     */
    @GetMapping("/places/search")
    public ResponseEntity<?> searchPlaces(@RequestParam String query) {
        return ResponseEntity.ok(placeService.searchPlaces(query));
    }

    /**
     * ✅ 장소 키워드 검색 (GET /courses/places/search)
     * - category 제거됨
     * 29p, 30p
     * 키워드 검색이랑 지도에서 찾기랑 같이 처리됨
     */
    @GetMapping("/places/search/map")
    public ResponseEntity<?> searchPlacesMap(@RequestParam String query) {
        return ResponseEntity.ok(placeService.searchPlaces(query));
    }

    /**
     * ✅ 장소 상세보기 (GET /courses/places/detail?query=장소명)
     * 31p, 32p
     */
    @GetMapping("/place/detail")
    public Map<String, Object> getPlaceDetail(
            @RequestParam String query,
            @RequestParam String sigunNm
    ) {
        return placeService.getPlaceDetailWithLocalpay(query, sigunNm);
    }


}
