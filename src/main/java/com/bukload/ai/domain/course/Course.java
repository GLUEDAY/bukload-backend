package com.bukload.ai.domain.course;

import com.bukload.auth.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- FK: User ----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String anchorId;         // 지역 앵커
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = true)
    private String regionName;       // 시군명 저장

    @Column(length = 20, nullable = true)
    private String travelDays;       // ⭐ "1일", "2일", "3일"

    @Column(nullable = true)
    private String imageUrl;         // ⭐ 대표 이미지 URL

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    @Builder.Default
    private List<CourseSegment> segments = new ArrayList<>();
}
