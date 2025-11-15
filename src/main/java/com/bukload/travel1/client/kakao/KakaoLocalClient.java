package com.bukload.travel1.client.kakao;

import com.bukload.travel1.dto.PlaceResponse;
import com.bukload.travel1.util.CategoryMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

    private final WebClient webClient;

    @Value("${app.providers.kakao.rest-api-key:}")
    private String restApiKey;

    private boolean enabled() { return StringUtils.hasText(restApiKey); }

    @PostConstruct
    void checkKeys() {
        System.out.println("[KakaoKey?] " + (restApiKey==null||restApiKey.isBlank()?"EMPTY":"****"+restApiKey.substring(Math.max(0, restApiKey.length()-4))));
    }

    /**
     * 🔹 단건 검색 (기존)
     */
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
                        .queryParam("query", query)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .bodyToMono(KakaoSearchResponse.class)
                .block();

        if (res == null || res.getDocuments() == null || res.getDocuments().isEmpty()) {
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
                        ? d.getRoad_address_name()
                        : d.getAddress_name())
                .rating(null)
                .reviewCount(null)
                .homepageUrl(null)
                .mapUrl(d.getPlace_url())
                .openNow(null)
                .phone(d.getPhone())
                .build());
    }

    /**
     * 🔹 다건 검색 (신규 추가)
     * - Kakao documents 전체를 PlaceResponse 리스트로 변환
     */
    public List<PlaceResponse> searchListByKeyword(String query) {
        if (!enabled()) {
            System.err.println("[Kakao] disabled (no restApiKey)");
            return List.of();
        }

        KakaoSearchResponse res = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("dapi.kakao.com")
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .bodyToMono(KakaoSearchResponse.class)
                .block();

        if (res == null || res.getDocuments() == null || res.getDocuments().isEmpty()) {
            return List.of();
        }

        List<PlaceResponse> list = new ArrayList<>();

        for (var d : res.getDocuments()) {
            list.add(
                    PlaceResponse.builder()
                            .placeId(d.getId())
                            .name(d.getPlace_name())
                            .category(CategoryMapper.fromKakaoGroup(d.getCategory_group_code()))
                            .lat(Double.parseDouble(d.getY()))
                            .lng(Double.parseDouble(d.getX()))
                            .address((d.getRoad_address_name() != null && !d.getRoad_address_name().isBlank())
                                    ? d.getRoad_address_name()
                                    : d.getAddress_name())
                            .rating(null)
                            .reviewCount(null)
                            .homepageUrl(null)
                            .mapUrl(d.getPlace_url())
                            .openNow(null)
                            .phone(d.getPhone())
                            .build()
            );
        }

        return list;
    }
}
