package com.bukload.course.service;

import com.bukload.ai.domain.course.Course;
import com.bukload.ai.domain.course.CourseSegment;
import com.bukload.course.dto.PlaceDto;
import com.bukload.localpaychecker.api.LocalpayApiService;
import com.bukload.travel1.dto.PlaceResponse;
import com.bukload.course.dto.SegmentRequest;
import com.bukload.ai.domain.course.CourseRepository;

import com.bukload.travel1.service.PlaceSearchServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceSearchServiceImpl placeSearchServiceImpl;
    private final CourseRepository courseRepository;
    private final LocalpayApiService localpayApiService;


    /**
     * ✅ 장소 검색 (팝업 내부)
     * - placeSearchServiceImpl.searchList(query)를 이용해
     *   카카오 + 구글 통합 검색 결과를 그대로 반환
     */
    public List<PlaceDto> searchPlaces(String query) {

        // 🔍 kakao/google 통합 검색 결과 호출
        List<PlaceDto> results = placeSearchServiceImpl.searchList(query);

        // 🔧 필요하면 여기서 필터링 또는 매핑 추가 가능
        // 예: 주소가 없는 경우 제외
        // results = results.stream()
        //         .filter(p -> p.getAddress() != null)
        //         .toList();

        return results;
    }
    /**
     * ✅ 장소 상세 조회
     * - 특정 placeId 또는 장소명(queryOrPlaceName)을 기준으로 상세 정보 조회
     * - placeSearchServiceImpl.searchOne() 호출
     */
    public Map<String, Object> getPlaceDetailWithLocalpay(String queryOrPlaceName, String sigunNm) {

        // 1️⃣ 장소 상세 조회
        PlaceResponse place = placeSearchServiceImpl.searchOne(queryOrPlaceName);

        // 2️⃣ 지역화폐 사용 가능 여부 체크
        boolean usable = false;
        try {
            usable = localpayApiService
                    .isLocalpayUsable(sigunNm, place.getName())
                    .block();
        } catch (Exception e) {
            log.warn("[getPlaceDetailWithLocalpay] Localpay check failed: {}", place.getName());
        }

        // 3️⃣ Map 형태로 조합하여 반환
        Map<String, Object> result = new HashMap<>();
        result.put("place", place);
        result.put("localpayUsable", usable);

        return result;
    }


}
