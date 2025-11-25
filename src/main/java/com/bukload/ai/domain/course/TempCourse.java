package com.bukload.ai.domain.course;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TempCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // PK

    private Long userId;             // 사용자 ID
    private String anchorId;         // 지역 앵커
    private String title;            // 코스 이름

    @Column(length = 1000)
    private String description;      // 코스 설명

    @Column(columnDefinition = "TEXT")
    private String placesJson;       // 장소 배열 JSON

    private LocalDateTime createdAt; // 생성 시각

    @Column(length = 100)
    private String regionName;       // 시군명 저장

    @Column(length = 20)
    private String travelDays;       // "1일", "2일", ...
}
