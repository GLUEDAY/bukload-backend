package com.bukload.ai.domain.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TempCourseRepository extends JpaRepository<TempCourse, Long> {

    /**
     * ✅ 특정 사용자의 임시 코스 전체 삭제
     * - recommendCourses 실행 시마다 기존 추천 결과 초기화
     */
    void deleteByUserId(Long userId);

    /**
     * ✅ 특정 사용자의 임시 코스 전체 조회
     * - 나중에 프론트에서 다시 불러올 때 사용
     */
    List<TempCourse> findByUserId(Long userId);


    /** ✅ 테이블 전체 초기화 + AUTO_INCREMENT 리셋 */
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE temp_course", nativeQuery = true)
    void truncateTable();
}
