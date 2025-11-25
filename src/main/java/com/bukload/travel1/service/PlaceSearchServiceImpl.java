// com/teamtiger/travel1/service/PlaceSearchServiceImpl.java
package com.bukload.travel1.service;

import com.bukload.course.dto.PlaceDto;
import com.bukload.travel1.client.google.GooglePlacesClient;
import com.bukload.travel1.client.kakao.KakaoLocalClient;
import com.bukload.travel1.dto.PlaceResponse;
import com.bukload.travel1.util.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

            // ✅ 대표사진 1장만 — 프록시 URL로 설정
            if (g.getRepresentativePhotoRef() != null) {
                String proxyUrl = "/api/places/photo?ref=" + g.getRepresentativePhotoRef() + "&w=800";
                base.setRepresentativePhotoUrl(proxyUrl);
            }
        }
        return base;
    }

    @Override
    public PlaceResponse searchOne(String name, String address) {
        // ✅ 카카오에는 "이름만"으로 검색 (주소는 넣지 않음)
        var kakaoOpt = kakao.searchOneByKeyword(name);
        if (kakaoOpt.isEmpty()) {
            throw new IllegalArgumentException("검색 결과가 없습니다: " + name);
        }
        var base = kakaoOpt.get();

        // ↓↓↓ 이 아래는 기존 searchOne(String query)와 동일하게 유지 ↓↓↓
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

            if (g.getRepresentativePhotoRef() != null) {
                String proxyUrl = "/api/places/photo?ref=" + g.getRepresentativePhotoRef() + "&w=800";
                base.setRepresentativePhotoUrl(proxyUrl);
            }
        }

        return base;
    }


    /**
     * Google 대표 사진을 photo_reference로 조회하고
     * 프록시 URL로 변환하여 반환하는 전용 메서드
     */
    public String getRepresentativePhotoUrl(String name, String address, double lat, double lng) {

        var gOpt = google.enrichByNameAndCoords(name, address, lat, lng);

        if (gOpt.isEmpty()) {
            return null;
        }

        var g = gOpt.get();
        String ref = g.getRepresentativePhotoRef();

        if (ref == null) {
            return null;
        }

        // 프록시 URL 생성
        return "/api/places/photo?ref=" + ref + "&w=800";
    }


    @Override
    public List<PlaceDto> searchList(String query) {

        // 1️⃣ Kakao에서 목록 조회
        List<PlaceResponse> kakaoResults = kakao.searchListByKeyword(query);

        if (kakaoResults.isEmpty()) {
            return List.of();
        }

        List<PlaceDto> dtos = new ArrayList<>();

        // 2️⃣ 각 결과마다 Google enrich 적용
        for (PlaceResponse base : kakaoResults) {

            var gOpt = google.enrichByNameAndCoords(
                    base.getName(),
                    base.getAddress(),
                    base.getLat(),
                    base.getLng()
            );

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

                if (g.getRepresentativePhotoRef() != null) {
                    String proxyUrl = "/api/places/photo?ref=" + g.getRepresentativePhotoRef() + "&w=800";
                    base.setRepresentativePhotoUrl(proxyUrl);
                }
            }

            // 3️⃣ PlaceResponse → PlaceDto 변환
            dtos.add(toDto(base));
        }

        return dtos;
    }

    // 🚀 여기 추가됨 — PlaceResponse → PlaceDto 변환기
    private PlaceDto toDto(PlaceResponse r) {
        return PlaceDto.builder()
                .placeId(r.getPlaceId())
                .name(r.getName())
                .category(r.getCategory())
                .lat(r.getLat())
                .lng(r.getLng())
                .address(r.getAddress())
                .build();
    }

}
