package com.teamtiger.travel1.service;

import com.teamtiger.travel1.client.google.GooglePlacesClient;
import com.teamtiger.travel1.client.kakao.KakaoLocalClient;
import com.teamtiger.travel1.dto.PlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceSearchServiceImpl implements PlaceSearchService {

    private final KakaoLocalClient kakao;
    private final GooglePlacesClient google;

    @Override
    public PlaceResponse searchOne(String query) {
        // 한국 검색 우선: 카카오 → 실패 시 구글
        var k = kakao.searchOneByKeyword(query);
        if (k.isPresent()) return k.get();

        var g = google.searchOneByText(query);
        if (g.isPresent()) return g.get();

        throw new IllegalArgumentException("검색 결과가 없습니다: " + query);
    }


}
