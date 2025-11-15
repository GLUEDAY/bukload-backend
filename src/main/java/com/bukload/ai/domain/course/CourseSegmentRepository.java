package com.bukload.ai.domain.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseSegmentRepository extends JpaRepository<CourseSegment, Long> {
}
