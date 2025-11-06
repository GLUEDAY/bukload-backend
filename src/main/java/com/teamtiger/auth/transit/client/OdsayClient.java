// com/teamtiger/auth/transit/client/OdsayClient.java
package com.teamtiger.auth.transit.client;

import com.teamtiger.auth.transit.client.dto.OdsaySearchPathResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Component
public class OdsayClient {

    private final WebClient webClient;
    private final String apiKey;

    public OdsayClient(
            @Value("${odsay.baseUrl}") String baseUrl,
            @Value("${odsay.apiKey}") String apiKey,
            @Value("${odsay.connectTimeoutMs:3000}") long connectTimeoutMs,
            @Value("${odsay.readTimeoutMs:5000}") long readTimeoutMs
    ) {
        this.apiKey = apiKey;

        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }
    @PostConstruct
    public void init() {
        System.out.println("[ODsay] apiKey present? " + (apiKey != null && !apiKey.isBlank()));
        System.out.println("[ODsay] apiKey = " + apiKey); // 필요시 실제 키도 출력 (개발용)
    }
    public Mono<OdsaySearchPathResponse> searchPath(
            double sx, double sy, double ex, double ey, Integer searchPathType
    ) {
        return webClient.get()
                .uri(uri -> uri.path("/searchPubTransPathT")
                        .queryParam("apiKey", apiKey)
                        .queryParam("SX", sx).queryParam("SY", sy)
                        .queryParam("EX", ex).queryParam("EY", ey)
                        .queryParam("OPT", 0)
                        .queryParam("SearchPathType", searchPathType == null ? 0 : searchPathType)
                        .build())
                .retrieve()
                .onStatus(s -> s.value() == 403, resp ->
                        resp.bodyToMono(String.class).map(body -> {
                            System.out.println("[ODsay 403 body] " + body);  // ← 원인 메시지 콘솔 확인
                            return new RuntimeException("ODsay 403: " + body);
                        }))
                .onStatus(s -> s.isError(), resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new RuntimeException("ODsay error: " + resp.statusCode() + " " + body)))
                .bodyToMono(OdsaySearchPathResponse.class);

    }
}
