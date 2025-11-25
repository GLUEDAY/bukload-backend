package com.bukload.ai.service.preset;

import com.bukload.ai.dto.response.RegionRecommendRes;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RegionPresetProvider {

    // 의정부, 구리, 양주, 동두천만
    private static final Map<String, RegionRecommendRes> PRESET = new LinkedHashMap<>();
    static {
        PRESET.put("uijeongbu", RegionRecommendRes.builder()
                .region("의정부시")
                .anchorId("uijeongbu")
                .comment("문화·전시와 휴식을 함께 즐기기 좋아요.")
                .tags(List.of("문화", "전시", "힐링"))
                .build());

        PRESET.put("guri", RegionRecommendRes.builder()
                .region("구리시")
                .anchorId("guri")
                .comment("서울 근교로 가볍게 나들이하기 좋아요.")
                .tags(List.of("서울근교", "가벼운코스"))
                .build());

        PRESET.put("yangju", RegionRecommendRes.builder()
                .region("양주시")
                .anchorId("yangju")
                .comment("감성 카페와 자연 풍경을 같이 즐길 수 있어요.")
                .tags(List.of("카페", "자연", "감성"))
                .build());

        PRESET.put("dongducheon", RegionRecommendRes.builder()
                .region("동두천시")
                .anchorId("dongducheon")
                .comment("조용하게 북부 쪽으로 다녀오기 좋아요.")
                .tags(List.of("북부", "조용한여행"))
                .build());
    }

    public Optional<RegionRecommendRes> findByAnchor(String anchorId) {
        if (anchorId == null) return Optional.empty();
        return Optional.ofNullable(PRESET.get(anchorId.toLowerCase()));
    }

    /**
     * LLM이 '의정부'처럼 우리 이름이랑 살짝 다르게 줘도
     * 4개 중 가장 가까운 걸로 스냅
     */
    public RegionRecommendRes snapToSupported(String nameOrAnchor) {
        if (nameOrAnchor == null) {
            // 디폴트는 의정부로 둘게 (기획 흐름이 의정부 예시였으니까)
            return PRESET.get("uijeongbu");
        }
        String lower = nameOrAnchor.toLowerCase();

        // 1) anchor로 바로 매칭
        if (PRESET.containsKey(lower)) return PRESET.get(lower);

        // 2) 한국어 이름 포함 매칭
        if (lower.contains("의정부")) return PRESET.get("uijeongbu");
        if (lower.contains("구리")) return PRESET.get("guri");
        if (lower.contains("양주")) return PRESET.get("yangju");
        if (lower.contains("동두천")) return PRESET.get("dongducheon");

        // 3) 그래도 모르겠으면 첫 번째(의정부)
        return PRESET.values().iterator().next();
    }

    public Collection<RegionRecommendRes> findAll() {
        return PRESET.values();
    }
}
