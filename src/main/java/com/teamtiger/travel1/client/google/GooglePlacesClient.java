package com.teamtiger.travel1.client.google;

import com.teamtiger.travel1.dto.PlaceResponse;
import com.teamtiger.travel1.util.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private final WebClient webClient;

    @Value("${app.providers.google.api-key:}")
    private String apiKey;

    private boolean enabled() { return StringUtils.hasText(apiKey); }

    public Optional<PlaceResponse> searchOneByText(String query) {
        if (!enabled()) {
            System.err.println("[Google] disabled (no apiKey)");
            return Optional.empty();
        }

        // 1) Text Search — uriBuilder로 자동 인코딩
        GoogleTextSearchResponse search = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/textsearch/json")
                        .queryParam("query", query)
                        .queryParam("language", "ko")
                        .queryParam("region", "KR")
                        .queryParam("key", apiKey)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(GoogleTextSearchResponse.class)
                .block();

        if (search == null) {
            System.err.println("[Google TextSearch] null response");
            return Optional.empty();
        }
        if (search.getStatus() != null && !"OK".equals(search.getStatus())) {
            System.err.println("[Google TextSearch] status=" + search.getStatus()
                    + " error=" + search.getError_message());
            return Optional.empty();
        }
        if (search.getResults() == null || search.getResults().isEmpty()) {
            System.err.println("[Google TextSearch] ZERO_RESULTS for query=" + query);
            return Optional.empty();
        }

        String placeId = search.getResults().get(0).getPlace_id();

        // 2) Details — website/url 등 수집
        GoogleDetailsResponse details = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/details/json")
                        .queryParam("place_id", placeId)
                        .queryParam("fields",
                                "place_id,name,types,geometry,formatted_address,rating,user_ratings_total,url,website")
                        .queryParam("language", "ko")
                        .queryParam("key", apiKey)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(GoogleDetailsResponse.class)
                .block();

        if (details == null) {
            System.err.println("[Google Details] null response");
            return Optional.empty();
        }
        if (details.getStatus() != null && !"OK".equals(details.getStatus())) {
            System.err.println("[Google Details] status=" + details.getStatus()
                    + " error=" + details.getError_message());
            return Optional.empty();
        }

        var r = details.getResult();
        Set<String> types = r.getTypes() != null ? new LinkedHashSet<>(r.getTypes()) : null;

        return Optional.of(PlaceResponse.builder()
                .placeId(r.getPlace_id())
                .name(r.getName())
                .category(CategoryMapper.fromGoogleTypes(types))
                .lat(r.getGeometry().getLocation().getLat())
                .lng(r.getGeometry().getLocation().getLng())
                .address(r.getFormatted_address())
                .rating(r.getRating() == null ? null : r.getRating().floatValue())
                .reviewCount(r.getUser_ratings_total())
                .homepageUrl(r.getWebsite()) // ✅ 공식 홈페이지
                .mapUrl(r.getUrl())          // ✅ 구글 지도 링크
                .build());
    }
}
