// com/teamtiger/travel1/service/PlaceSearchServiceImpl.java
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
        var kakaoOpt = kakao.searchOneByKeyword(query);
        if (kakaoOpt.isEmpty()) {
            throw new IllegalArgumentException("검색 결과가 없습니다: " + query);
        }
        var base = kakaoOpt.get();

        var gOpt = google.enrichByNameAndCoords(base.getName(), base.getAddress(), base.getLat(), base.getLng());
        if (gOpt.isPresent()) {
            var g = gOpt.get();

            base.setRating(g.getRating());
            base.setReviewCount(g.getReviewCount());
            base.setHomepageUrl(g.getWebsite());
            base.setMapUrl(g.getMapUrl() != null ? g.getMapUrl() : base.getMapUrl());
            base.setOpenNow(g.getOpenNow());
            base.setOpeningHoursText(g.getWeekdayText());

            if (base.getCategory() == null) {
                base.setCategory(CategoryMapper.fromGoogleTypes(g.getGoogleCategoryTypes()));
            }
            if (base.getPhone() == null || base.getPhone().isBlank()) {
                if (g.getFormattedPhone() != null && !g.getFormattedPhone().isBlank()) {
                    base.setPhone(g.getFormattedPhone());
                }
            }

            // ✅ 대표사진 1장만 — 프록시 URL로 설정 (API Key 노출 없음)
            if (g.getRepresentativePhotoRef() != null) {
                String proxyUrl = "/api/places/photo?ref=" + g.getRepresentativePhotoRef() + "&w=800";
                base.setRepresentativePhotoUrl(proxyUrl);
            }
        }
        return base;
    }
}
