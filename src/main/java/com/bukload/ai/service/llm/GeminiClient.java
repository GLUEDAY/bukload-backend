package com.bukload.ai.service.llm;

import com.bukload.ai.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final WebClient geminiWebClient;
    private final GeminiProperties props;

    /**
     * 실제 Google Gemini API 호출
     * API 오류 발생 시 RuntimeException을 던지도록 수정하여 서비스 계층으로 전파
     */
    public Mono<String> generate(String model, String prompt) {
        // ✅ 설정 누락 방지
        if (props.getEndpoint() == null || props.getApiKey() == null || model == null) {
            System.out.println("[GeminiClient] ❌ endpoint/model/apiKey 중 null 존재");
            // API 키가 없으면 바로 오류 발생
            return Mono.error(new IllegalArgumentException("Gemini API configuration is incomplete."));
        }

        // ✅ 호출 URL 구성
        String url = props.getEndpoint()
                + "/models/" + model + ":generateContent?key=" + props.getApiKey();

        // ✅ 요청 body
        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("role", "user",
                                "parts", new Object[]{Map.of("text", prompt)})
                }
        );

        System.out.println("\n[GeminiClient] 🚀 요청 URL: " + url);
        System.out.println("[GeminiClient] 🧠 프롬프트 내용 ↓↓↓");
        System.out.println(prompt);

        // ✅ WebClient 요청 + 응답 로깅 및 오류 전파
        return geminiWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                // ❗ 4xx/5xx 오류 시 WebClientResponseException 던지도록
                .onStatus(status -> status.isError(), clientResponse -> {
                    System.out.println("[GeminiClient] ❗ API 응답 오류: " + clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(bodyString -> Mono.error(new RuntimeException(
                                    "Gemini API Error: " + clientResponse.statusCode() + " - " + bodyString)));
                })
                .bodyToMono(Map.class)
                .map(resp -> {
                    try {
                        System.out.println("\n[GeminiClient] ✅ 원본 응답 전체 ↓↓↓");
                        System.out.println(resp);

                        // candidates → content → parts → text 추출
                        var candidates = (java.util.List<Map<String, Object>>) resp.get("candidates");
                        if (candidates == null || candidates.isEmpty()) {
                            System.out.println("[GeminiClient] ⚠️ candidates 비어 있음 → 내용 없음으로 간주");
                            return "";
                        }

                        var content = (Map<String, Object>) candidates.get(0).get("content");
                        var parts = (java.util.List<Map<String, Object>>) content.get("parts");
                        if (parts == null || parts.isEmpty()) {
                            System.out.println("[GeminiClient] ⚠️ parts 비어 있음 → 내용 없음으로 간주");
                            return "";
                        }

                        String text = String.valueOf(parts.get(0).get("text"));
                        System.out.println("\n[GeminiClient] 💬 모델이 반환한 텍스트 ↓↓↓");
                        System.out.println(text);
                        System.out.println("-----------------------------------------------------");

                        return text;
                    } catch (Exception e) {
                        System.out.println("[GeminiClient] ⚠️ 응답 파싱 중 예외: " + e.getMessage());
                        // 파싱 실패 시 예외를 던지도록 수정
                        throw new RuntimeException("Gemini Response parsing failed: " + e.getMessage());
                    }
                })
                .doOnError(err -> {
                    System.out.println("[GeminiClient] ❗ API 호출 오류 발생 ↓↓↓");
                    err.printStackTrace();
                });
    }
}