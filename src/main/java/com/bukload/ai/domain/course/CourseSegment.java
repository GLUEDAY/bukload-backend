package com.bukload.ai.domain.course;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseSegment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id")
    private Course course;

    private Integer orderNo;

    private String placeId;      // 외부 place id (kakao/google)
    private String placeName;
    private String category;
    private Double lat;
    private Double lng;

    private String transportMode; // "WALK","CAR","TRANSIT" 등
}
