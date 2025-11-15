// com/teamtiger/travel1/controller/PhotoProxyController.java
package com.bukload.travel1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PhotoProxyController {

    private final WebClient webClient;

    @Value("${app.providers.google.api-key}")
    private String apiKey;

    @GetMapping("/photo")
    public Mono<ResponseEntity<byte[]>> photo(
            @RequestParam("ref") String ref,
            @RequestParam(value = "w", defaultValue = "800") int maxWidth
    ) {
        String googlePhotoUrl = UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/place/photo")
                .queryParam("maxwidth", maxWidth)
                .queryParam("photo_reference", ref)
                .queryParam("key", apiKey) // 서버에서만 사용 → 프론트 노출 X
                .build(true)
                .toUriString();

        return webClient.get()
                .uri(googlePhotoUrl)
                .retrieve()
                .toEntity(byte[].class)
                .map(res -> {
                    HttpHeaders headers = new HttpHeaders();
                    MediaType ct = res.getHeaders().getContentType();
                    headers.setContentType(ct != null ? ct : MediaType.IMAGE_JPEG);
                    headers.setCacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)).cachePublic());
                    return new ResponseEntity<>(res.getBody(), headers, HttpStatus.OK);
                });
    }
}
