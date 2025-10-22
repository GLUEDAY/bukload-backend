package com.teamtiger.travel1.client.kakao;

import com.teamtiger.travel1.dto.PlaceResponse;
import com.teamtiger.travel1.util.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

    private final WebClient webClient;

    @Value("${app.providers.kakao.rest-api-key:}")
    private String restApiKey;

    private boolean enabled() { return StringUtils.hasText(restApiKey); }

    public Optional<PlaceResponse> searchOneByKeyword(String query) {
        if (!enabled()) {
            System.err.println("[Kakao] disabled (no restApiKey)");
            return Optional.empty();
        }

        KakaoSearchResponse res = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("dapi.kakao.com")
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", query) // 자동 인코딩
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .bodyToMono(KakaoSearchResponse.class)
                .block();

        if (res == null) {
            System.err.println("[Kakao] null response");
            return Optional.empty();
        }
        if (res.getDocuments() == null || res.getDocuments().isEmpty()) {
            System.err.println("[Kakao] ZERO_RESULTS for query=" + query);
            return Optional.empty();
        }

        var d = res.getDocuments().get(0);

        return Optional.of(PlaceResponse.builder()
                .placeId(d.getId())
                .name(d.getPlace_name())
                .category(CategoryMapper.fromKakaoGroup(d.getCategory_group_code()))
                .lat(Double.parseDouble(d.getY()))
                .lng(Double.parseDouble(d.getX()))
                .address((d.getRoad_address_name() != null && !d.getRoad_address_name().isBlank())
                        ? d.getRoad_address_name() : d.getAddress_name())
                .rating(null)          // 카카오는 평점/리뷰수/홈페이지 미제공
                .reviewCount(null)
                .homepageUrl(null)
                .mapUrl(d.getPlace_url()) // ✅ 카카오 지도 링크
                .build());
    }
}
