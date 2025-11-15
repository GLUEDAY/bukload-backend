// com/teamtiger/travel1/client/google/GooglePlacesClient.java
package com.bukload.travel1.client.google;

import jakarta.annotation.PostConstruct;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
                .onStatus(s -> s.value() == 403, cr -> cr.bodyToMono(String.class)
                        .map(body -> new RuntimeException("[Google TextSearch 403] " + body)))
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), cr -> cr.bodyToMono(String.class)
                        .map(body -> new RuntimeException("[Google TextSearch " + cr.statusCode().value() + "] " + body)))
                .bodyToMono(GoogleTextSearchResponse.class)
                .block();

        if (search == null || search.getResults() == null || search.getResults().isEmpty()
                || (search.getStatus() != null && !"OK".equals(search.getStatus()))) {
            System.err.println("[Google TextSearch] status=" + (search==null? "null":search.getStatus())
                    + " error=" + (search==null? "null":search.getError_message()));
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
                        .queryParam("fields", String.join(",",
                                "place_id","name","types","geometry","formatted_address",
                                "rating","user_ratings_total","url","website",
                                "formatted_phone_number",
                                // ✅ photos만 추가 (대표사진 1장만 쓸거라 ref만 필요)
                                "photos","photos.photo_reference",
                                "opening_hours","opening_hours.weekday_text","opening_hours.periods",
                                "current_opening_hours","current_opening_hours.weekday_text","current_opening_hours.periods"
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

        if (details == null || (details.getStatus()!=null && !"OK".equals(details.getStatus()))) {
            System.err.println("[Google Details] status=" + (details==null? "null" : details.getStatus())
                    + " error=" + (details==null? "null" : details.getError_message()));
            return Optional.empty();
        }

        var r = details.getResult();
        Set<String> types = (r.getTypes() != null) ? new LinkedHashSet<>(r.getTypes()) : null;

        var weekdayText =
                (r.getCurrent_opening_hours() != null && r.getCurrent_opening_hours().getWeekday_text() != null && !r.getCurrent_opening_hours().getWeekday_text().isEmpty())
                        ? r.getCurrent_opening_hours().getWeekday_text()
                        : (r.getOpening_hours() != null ? r.getOpening_hours().getWeekday_text() : null);

        // ✅ 대표사진 ref만 추출
        String representativePhotoRef = null;
        if (r.getPhotos() != null && !r.getPhotos().isEmpty() && r.getPhotos().get(0).getPhoto_reference() != null) {
            representativePhotoRef = r.getPhotos().get(0).getPhoto_reference();
        }

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
                .weekdayText(weekdayText)
                .formattedPhone(r.getFormatted_phone_number())
                .representativePhotoRef(representativePhotoRef)
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
        private Set<String> googleCategoryTypes;
        private List<String> weekdayText;
        private String formattedPhone;

        // ✅ 대표사진 ref (단일)
        private String representativePhotoRef;
    }
}
