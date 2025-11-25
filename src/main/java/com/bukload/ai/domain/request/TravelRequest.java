package com.bukload.ai.domain.request;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TravelRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long themeId;

    private String departureLocation;
    private Integer travelDays;
    private Integer budget;

    private String gender;         // ex) "F","M","OTHER"
    private LocalDate birthDate;

    private String companions;     // ex) "혼자", "친구", "가족"
    private String style;          // ex) "자연, 카페 위주"
    private String additionalRequest;

    // 선택된 지역(Anchor) 고정 시 저장
    private String anchorId;       // ex) "yangpyeong"
    private String regionName;     // ex) "양평군"
}
