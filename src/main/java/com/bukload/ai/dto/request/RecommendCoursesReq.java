package com.bukload.ai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecommendCoursesReq {
    @NotNull
    private Long requestId;
    private String anchorId; // 지역 선택(anchor) - region API 결과를 사용
}
