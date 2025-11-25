package com.bukload.ai.domain.course;

import com.bukload.auth.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUser(User user);
    List<Course> findByUserId(Long userId);

}
