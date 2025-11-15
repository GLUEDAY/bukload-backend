package com.bukload.ai.domain.course;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseSegment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    private Integer orderNo;

    private String placeId;      // 외부 place id (kakao/google)
    private String placeName;
    private String category;
    private Double lat;
    private Double lng;

    @Column(nullable = true)
    private String transportMode; // "WALK","CAR","TRANSIT" 등


    @Column(columnDefinition = "TEXT")
    private String photoUrl;


    @Column(columnDefinition = "TEXT")
    private String transitJson; // ✅ ODsay JSON 문자열 그대로 저장

    // ============================================
    // ⭐ 새로 추가된 필드들 (요청 사항)
    // ============================================
    @Column(nullable = true)
    private Boolean localpayOX;      // 지역화폐 사용 가능 여부
}
