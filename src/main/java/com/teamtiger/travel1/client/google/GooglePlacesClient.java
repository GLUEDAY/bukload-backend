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
import jakarta.annotation.PostConstruct;


@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private final WebClient webClient;

    @Value("${app.providers.google.api-key:}")
    private String apiKey;

    private boolean enabled() { return StringUtils.hasText(apiKey); }

    @PostConstruct
    void checkGoogleKey() {
        System.out.println("[GooglePlacesClient] API key loaded? " +
                (apiKey == null || apiKey.isBlank()
                        ? "❌ EMPTY or not loaded"
                        : "✅ ****" + apiKey.substring(Math.max(0, apiKey.length() - 4))));
    }
    public Optional<GoogleEnrichment> enrichByNameAndCoords(String name, String address, double lat, double lng) {
        if (!enabled()) {
            System.err.println("[Google] disabled (no apiKey)");
            return Optional.empty();
        }

        String query = (address == null || address.isBlank()) ? name : (name + " " + address);

        // ---------- Text Search ----------
        GoogleTextSearchResponse search = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/textsearch/json")
                        .queryParam("query", query)
                        .queryParam("language", "ko")
                        .queryParam("region", "KR")
                        .queryParam("location", lat + "," + lng)
                        .queryParam("radius", 1200)
                        .queryParam("key", apiKey)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                // ✅ 여기 추가: 403/4xx/5xx 응답 바디 로깅
                .onStatus(s -> s.value() == 403, cr -> cr.bodyToMono(String.class)
                        .map(body -> new RuntimeException("[Google TextSearch 403] " + body)))
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), cr -> cr.bodyToMono(String.class)
                        .map(body -> new RuntimeException("[Google TextSearch " + cr.statusCode().value() + "] " + body)))
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

        // ---------- Details ----------
        GoogleDetailsResponse details = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/details/json")
                        .queryParam("place_id", placeId)
                        // ✅ opening_hours / current_opening_hours 모두 요청
                        .queryParam("fields",
                                String.join(",",
                                        "place_id","name","types","geometry","formatted_address",
                                        "rating","user_ratings_total","url","website",
                                        "opening_hours",
                                        "opening_hours.weekday_text",
                                        "opening_hours.periods",
                                        "current_opening_hours",
                                        "current_opening_hours.weekday_text",
                                        "current_opening_hours.periods"
                                ))
                        .queryParam("language", "ko")
                        .queryParam("key", apiKey)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(s -> s.value() == 403, cr -> cr.bodyToMono(String.class)
                        .map(body -> new RuntimeException("[Google Details 403] " + body)))
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), cr -> cr.bodyToMono(String.class)
                        .map(body -> new RuntimeException("[Google Details " + cr.statusCode().value() + "] " + body)))
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
        java.util.Set<String> types =
                (r.getTypes() != null) ? new java.util.LinkedHashSet<>(r.getTypes()) : null;

        // ✅ 텍스트 우선순위: current_opening_hours -> opening_hours
        var weekdayText =
                (r.getCurrent_opening_hours() != null && r.getCurrent_opening_hours().getWeekday_text() != null && !r.getCurrent_opening_hours().getWeekday_text().isEmpty())
                        ? r.getCurrent_opening_hours().getWeekday_text()
                        : (r.getOpening_hours() != null ? r.getOpening_hours().getWeekday_text() : null);

        return Optional.of(GoogleEnrichment.builder()
                .googlePlaceId(r.getPlace_id())
                .rating(r.getRating() == null ? null : r.getRating().floatValue())
                .reviewCount(r.getUser_ratings_total())
                .website(r.getWebsite())
                .mapUrl(r.getUrl())
                .openNow(
                        r.getCurrent_opening_hours() != null
                                ? r.getCurrent_opening_hours().getOpen_now()
                                : (r.getOpening_hours() != null ? r.getOpening_hours().getOpen_now() : null)
                )
                .googleCategoryTypes(types)
                .weekdayText(weekdayText) // ✅ 추가
                .build());
    }


    @Data @Builder
    @AllArgsConstructor
    public static class GoogleEnrichment {
        private String googlePlaceId;
        private Float rating;
        private Integer reviewCount;
        private String website;
        private String mapUrl;
        private Boolean openNow;
        private java.util.Set<String> googleCategoryTypes;
        // ✅ 추가: 요일별 텍스트
        private java.util.List<String> weekdayText;
    }
}
