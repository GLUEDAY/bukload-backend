package com.bukload.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.gemini")
public class GeminiProperties {
    private String endpoint;     // https://generativelanguage.googleapis.com/v1beta
    private String modelRegion;  // gemini-1.5-flash
    private String modelCourses; // gemini-1.5-flash
    private String apiKey;       // 환경변수에서
}
