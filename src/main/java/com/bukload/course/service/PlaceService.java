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
    public List<PlaceDto> searchPlaces(String query, String sigunNm, Long courseId) {

        // 0️⃣ 시군명 우선순위: 요청 sigunNm > course.regionName > 없음
        String effectiveSigun = null;
        if (sigunNm != null && !sigunNm.isBlank()) {
            effectiveSigun = sigunNm;
        } else if (courseId != null) {
            var opt = courseRepository.findById(courseId);
            if (opt.isPresent()) {
                String rn = opt.get().getRegionName();
                if (rn != null && !rn.isBlank()) {
                    effectiveSigun = rn;
                }
            } else {
                log.warn("[searchPlaces] course not found for id={}", courseId);
            }
        }

        // 🔍 kakao/google 통합 검색 결과 호출 (시군명이 있으면 검색어에 함께 붙여 정확도 ↑)
        String searchQuery = (effectiveSigun != null && !effectiveSigun.isBlank())
                ? query + " " + effectiveSigun
                : query;
        List<PlaceDto> results = placeSearchServiceImpl.searchList(searchQuery);

        // 🔧 지역화폐 사용 가능 여부 세팅 (effectiveSigun가 있으면 체크, 없으면 false)
        boolean hasSigun = effectiveSigun != null && !effectiveSigun.isBlank();
        for (PlaceDto dto : results) {
            boolean usable = false;
            if (hasSigun) {
                try {
                    usable = localpayApiService
                            .isLocalpayUsable(effectiveSigun, dto.getName())
                            .block();
                } catch (Exception e) {
                    log.warn("[searchPlaces] Localpay check failed: {}", dto.getName());
                }
            }
            dto.setLocalpayOX(usable);
        }

        return results;
    }
     // ✅ 장소 상세 조회
     public Map<String, Object> getPlaceDetailWithLocalpay(String queryOrPlaceName, String sigunNm) {

        // 1️⃣ 장소 상세 조회
         PlaceResponse place = placeSearchServiceImpl.searchOne(queryOrPlaceName, sigunNm);

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
