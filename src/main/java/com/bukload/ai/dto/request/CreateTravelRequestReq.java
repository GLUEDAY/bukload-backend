package com.bukload.ai.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTravelRequestReq {
    private Long themeId;
    @NotBlank
    private String departureLocation;
    @NotNull @Min(1) @Max(14)
    private Integer travelDays;
    @NotNull @Min(0)
    private Integer budget;

    @NotBlank
    private String gender;          // F/M/OTHER
    @NotNull
    private LocalDate birthDate;

    @NotBlank
    private String companions;      // 친구/가족/혼자
    @NotBlank
    private String style;           // 예: "자연, 카페"
    private String additionalRequest;
}
