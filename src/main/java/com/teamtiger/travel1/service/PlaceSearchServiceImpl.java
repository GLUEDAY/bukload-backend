package com.teamtiger.travel1.service;

import com.teamtiger.travel1.client.google.GooglePlacesClient;
import com.teamtiger.travel1.client.kakao.KakaoLocalClient;
import com.teamtiger.travel1.dto.PlaceResponse;
import com.teamtiger.travel1.util.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceSearchServiceImpl implements PlaceSearchService {

    private final KakaoLocalClient kakao;
    private final GooglePlacesClient google;

    @Override
    public PlaceResponse searchOne(String query) {
        // 1) 한국 검색 우선: 카카오
        var kakaoOpt = kakao.searchOneByKeyword(query);
        if (kakaoOpt.isEmpty()) {
            // 2) 카카오 실패 시: 구글 단독(이전 로직 유지하고 싶다면 여기에 google.searchOneByText 추가 가능)
            throw new IllegalArgumentException("검색 결과가 없습니다: " + query);
        }

        var base = kakaoOpt.get(); // 카카오 결과를 기본 바디로 사용

        // 3) 구글로 필요한 필드만 보강(리뷰/리뷰수/홈페이지/지도링크/영업중 여부)
        var gOpt = google.enrichByNameAndCoords(base.getName(), base.getAddress(), base.getLat(), base.getLng());
        if (gOpt.isPresent()) {
            var g = gOpt.get();
            // placeId는 그대로 카카오 id 유지(대외 응답 일관). 필요 시 googlePlaceId 별도 항목을 만들거나 교체 가능
            base.setRating(g.getRating());
            base.setReviewCount(g.getReviewCount());
            base.setHomepageUrl(g.getWebsite());
            base.setMapUrl(g.getMapUrl() != null ? g.getMapUrl() : base.getMapUrl());
            base.setOpenNow(g.getOpenNow());

            // 카테고리 비어있다면 구글 타입으로 보완
            if (base.getCategory() == null) {
                base.setCategory(CategoryMapper.fromGoogleTypes(g.getGoogleCategoryTypes()));
            }
        }

        return base;
    }
}
