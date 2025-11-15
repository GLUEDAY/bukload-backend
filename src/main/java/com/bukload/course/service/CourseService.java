package com.bukload.course.service;

import com.bukload.ai.domain.course.Course;
import com.bukload.ai.domain.course.CourseSegment;

import com.bukload.ai.dto.dto.CourseSegmentForCourseDetail;
import com.bukload.course.dto.PlaceDto;
import com.bukload.course.dto.SegmentRequest;
import com.bukload.course.dto.SavedCourseDto;

import com.bukload.ai.domain.course.CourseRepository;

import com.bukload.auth.user.User;

import com.bukload.localpaychecker.api.LocalpayApiService;
import com.bukload.transit.TransitService;
import com.bukload.transit.client.dto.TransitRouteResponse;
import com.bukload.travel1.service.PlaceSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final PlaceSearchService placeSearchService;
    private final TransitService transitService;
    private final ObjectMapper mapper;
    private final LocalpayApiService localpayApiService;


    /**
     * ✅ 내 코스 목록 조회
     */
    public List<SavedCourseDto> getMyCourses(User user) {
        return courseRepository.findByUser(user)
                .stream()
                .map(SavedCourseDto::from)
                .toList();
    }

    /**
     * ✅ 전체 코스 목록 조회
     */
    public List<SavedCourseDto> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(SavedCourseDto::from)
                .toList();
    }

    /**
     * ✅ 코스 상세 조회
     */
    public Map<String, Object> getCourseDetail(Long courseId) {

        // 1) 코스 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스가 존재하지 않습니다."));

        // 2) segments를 DTO로 변환
        List<CourseSegmentForCourseDetail> segmentDtos =
                course.getSegments().stream()
                        .map(CourseSegmentForCourseDetail::from)
                        .toList();

        // 3) Map 형태로 묶어서 반환
        Map<String, Object> result = new HashMap<>();
        result.put("title", course.getTitle());
        result.put("segments", segmentDtos);

        return result;
    }



    /**
     * ✅ 장소 순서 변경 + 삭제 (저장 버튼 클릭 시)
     * 요청 리스트(newOrderList)에 포함되지 않은 segmentId는 삭제 처리
     * 나머지는 순서(orderNo) 재정렬
     */
    @Transactional
    public void updateSegmentOrderAndDelete(Long courseId, List<Long> newOrderList) {
        // 1️⃣ 코스 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스가 존재하지 않습니다."));

        // 2️⃣ 기존 segment 목록 가져오기
        List<CourseSegment> existingSegments = course.getSegments();

        // 3️⃣ 요청으로 들어온 segmentId 집합
        Set<Long> newOrderSet = new HashSet<>(newOrderList);

        // 4️⃣ 요청에 없는 segment → 삭제 처리
        List<CourseSegment> toRemove = existingSegments.stream()
                .filter(segment -> !newOrderSet.contains(segment.getId()))
                .toList();

        existingSegments.removeAll(toRemove);

        // 5️⃣ 순서 재정렬
        int order = 1;
        for (Long segmentId : newOrderList) {
            for (CourseSegment segment : existingSegments) {
                if (segment.getId().equals(segmentId)) {
                    segment.setOrderNo(order++);
                    break;
                }
            }
        }

        // 6️⃣ 삭제된 segment는 orphanRemoval=true 로 인해 DB에서도 자동 삭제됨
        // 7️⃣ courseRepository.save(course); // @Transactional 이므로 자동 flush
    }

    /**
     * ✅ 장소 추가 (팝업에서 호출)
     * 팝업에서 선택된 장소를 코스에 연결하는 역할
     * → 프론트에서 courseId, placeId, transportMode, orderNo를 함께 보냄
     */
    @Transactional
    public void addSegmentFromPopup(Long courseId, String placeName) {

        // 1️⃣ 코스 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스가 존재하지 않습니다."));

        // 2️⃣ 코스에 저장된 regionName 활용 (TravelRequest 필요 없음)
        String sigunNm = course.getRegionName();   // ⭐ 이걸 그대로 사용

        // 3️⃣ 자동 orderNo 설정
        int nextOrder = course.getSegments().size() + 1;

        // 4️⃣ 외부 API (카카오+구글)
        PlaceDto detail = PlaceDto.fromSearchResult(
                placeSearchService.searchOne(placeName)
        );

        // 5️⃣ 지역화폐 사용 가능 여부 체크
        boolean localpayUsable = false;
        try {
            localpayUsable = localpayApiService
                    .isLocalpayUsable(sigunNm, placeName)
                    .block();
        } catch (Exception e) {
            System.out.println("[addSegmentFromPopup] ⚠️ Localpay check failed: " + placeName);
        }

        // 6️⃣ 새로운 Segment 생성
        CourseSegment newSegment = CourseSegment.builder()
                .course(course)
                .placeId(detail.getPlaceId())
                .placeName(detail.getName())
                .category(detail.getCategory())
                .lat(detail.getLat())
                .lng(detail.getLng())
                .orderNo(nextOrder)
                .localpayOX(localpayUsable)      // ⭐ 여기에 정확한 값 넣기
                .transportMode(null)             // 지금 단계에서는 입력 없음
                .transitJson(null)               // 지금 단계에서는 입력 없음
                .build();

        // 7️⃣ 코스에 추가
        course.getSegments().add(newSegment);
    }



    /**
     * ✅ 장소 수정 버튼 중에 대중교통 정보 업데이트 담당 로직
     * ODsay api로 대중교통 정보도 업데이트함
     */
    @Transactional
    public void updateTransitJson(Long courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스가 존재하지 않습니다."));

        List<CourseSegment> segments = course.getSegments();

        if (segments.size() < 1) return;

        for (int i = 0; i < segments.size(); i++) {
            CourseSegment current = segments.get(i);
            CourseSegment next = (i + 1 < segments.size()) ? segments.get(i + 1) : null;

            if (next != null) {

                try {
                    // ODsay API 호출
                    TransitRouteResponse route = transitService
                            .searchBestRoute(
                                    current.getLng(), current.getLat(),
                                    next.getLng(), next.getLat(),
                                    0
                            )
                            .block();

                    if (route != null) {
                        String routeJson = mapper.writeValueAsString(route);
                        current.setTransitJson(routeJson);
                    }

                } catch (Exception e) {
                    System.out.println("[updateTransitJson] ⚠️ ODsay 호출 실패: " + e.getMessage());
                    current.setTransitJson(null);
                }

            } else {
                // 마지막 장소
                current.setTransitJson(null);
            }
        }
    }



}
