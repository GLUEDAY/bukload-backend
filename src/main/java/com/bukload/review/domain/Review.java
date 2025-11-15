package com.bukload.review.domain;

import com.bukload.ai.domain.course.CourseSegment;
import com.bukload.auth.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- FK: User ----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ---------- FK: CourseSegment ----------
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_segment_id", nullable = false, unique = true)
    private CourseSegment courseSegment;


    // ======================================================
    // ⭐ 새로 추가되는 스냅샷 필드들
    // ======================================================

    @Column(length = 200)
    private String placeName;   // 장소 이름 (segment.placeName 스냅샷)

    @Column(length = 200)
    private String courseTitle; // 코스 이름

    @Column(length = 100)
    private String region;      // anchorId

    @Column(length = 50)
    private String travelDays;  // "1일", "2일", "3일" 같은 문자열


    // ======================================================
    //   파일 및 텍스트 정보
    // ======================================================
    @Column(columnDefinition = "TEXT")
    private String content;          // 리뷰 텍스트

    @Column(name = "s3_key", length = 500)
    private String s3Key;            // S3 key

    @Column(name = "content_type", length = 100)
    private String contentType;      // image/png, text/plain 등

    @Column(name = "size_bytes")
    private Long sizeBytes;          // 파일 크기(Byte)
}
