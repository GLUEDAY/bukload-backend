package com.teamtiger.travel1.client.google;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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

    /**
     * 카카오로 잡은 후보(name, address, lat, lng)를 바탕으로
     * Google Text Search(+location bias)로 place_id 매칭 → Details 일부 필드만 조회.
     */
    public Optional<GoogleEnrichment> enrichByNameAndCoords(String name, String address, double lat, double lng) {
        if (!enabled()) {
            System.err.println("[Google] disabled (no apiKey)");
            return Optional.empty();
        }

        // 1) Text Search with location bias (결과 매칭 정확도 ↑)
        //    참고: Text Search는 location+radius로 결과를 '바이어스' 할 수 있음.
        //    query는 name + address를 합쳐 정확도 향상.
        String query = (address == null || address.isBlank()) ? name : (name + " " + address);

        GoogleTextSearchResponse search = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/textsearch/json")
                        .queryParam("query", query)
                        .queryParam("language", "ko")
                        .queryParam("region", "KR")
                        .queryParam("location", lat + "," + lng) // 위치 바이어스
                        .queryParam("radius", 1200)              // 1.2km 반경
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

        // 2) Details — 필요한 필드만 최소 조회(비용 절감)
        GoogleDetailsResponse details = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/details/json")
                        .queryParam("place_id", placeId)
                        .queryParam("fields", "place_id,name,types,geometry,formatted_address,rating,user_ratings_total,url,website,current_opening_hours")
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

        return Optional.of(GoogleEnrichment.builder()
                .googlePlaceId(r.getPlace_id())
                .rating(r.getRating() == null ? null : r.getRating().floatValue())
                .reviewCount(r.getUser_ratings_total())
                .website(r.getWebsite())
                .mapUrl(r.getUrl())
                .openNow(r.getCurrent_opening_hours() == null ? null : r.getCurrent_opening_hours().getOpen_now())
                .googleCategoryTypes(types)
                .build());
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class GoogleEnrichment {
        private String googlePlaceId;
        private Float rating;
        private Integer reviewCount;
        private String website;
        private String mapUrl;
        private Boolean openNow;
        private Set<String> googleCategoryTypes; // 필요 시 내부 매핑에 활용 가능
    }
}
