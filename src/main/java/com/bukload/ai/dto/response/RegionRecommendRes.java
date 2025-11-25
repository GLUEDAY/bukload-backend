package com.bukload.ai.dto.response;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegionRecommendRes {
    private String region;      // 예: "양평군"
    private String anchorId;    // 예: "yangpyeong"
    private String comment;     // AI 코멘트
    private List<String> tags;  // ["자연","카페","감성"]
}
