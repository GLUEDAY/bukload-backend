package com.bukload.ai.domain.course;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requestId;          // TravelRequest.id (연결)

    private String anchorId;         // 지역 앵커
    private String title;
    @Column(length = 1000)
    private String description;

    private Double totalDistanceKm;  // 계산된 총 거리
    private Integer estimatedMinutes;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    @Builder.Default
    private List<CourseSegment> segments = new ArrayList<>();
}
