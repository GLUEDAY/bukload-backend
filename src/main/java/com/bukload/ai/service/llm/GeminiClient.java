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
     * 지정된 모델과 프롬프트를 사용하여 Gemini API에 콘텐츠 생성을 요청합니다.
     * * @param model 사용할 Gemini 모델의 이름 (예: "gemini-2.5-flash")
     * @param prompt 모델에 전달할 사용자 입력 텍스트
     * @return 생성된 텍스트 응답을 포함하는 Mono<String>
     */
    public Mono<String> generate(String model, String prompt) {
        // 1) 설정이 비어 있으면 바로 fallback
        if (props.getEndpoint() == null || props.getApiKey() == null || model == null) {
            return Mono.just("");
        }

        // URL 구성: Endpoint + /models/{model}:generateContent?key={apiKey}
        String url = props.getEndpoint()
                + "/models/" + model + ":generateContent?key=" + props.getApiKey();

        // 요청 본문(Body) 구성
        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("role", "user",
                                "parts", new Object[]{Map.of("text", prompt)})
                }
        );

        return geminiWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    try {
                        // 응답에서 생성된 텍스트를 파싱
                        var candidates = (java.util.List<Map<String, Object>>) resp.get("candidates");
                        if (candidates == null || candidates.isEmpty()) return "";
                        var content = (Map<String, Object>) candidates.get(0).get("content");
                        var parts = (java.util.List<Map<String, Object>>) content.get("parts");
                        if (parts == null || parts.isEmpty()) return "";
                        return String.valueOf(parts.get(0).get("text"));
                    } catch (Exception e) {
                        // 파싱 중 오류 발생 시 빈 문자열 반환
                        return "";
                    }
                })
                // 2) 여기서 외부 API가 404/401/500/타임아웃을 던져도 그냥 빈 문자열로
                .onErrorReturn("");
    }
}