package com.bukload.mypage.dto;

import com.bukload.course.dto.SavedCourseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MyPageResponse {

    private int userPoint;                     // 유저 포인트
    private List<SavedCourseDto> savedCourses; // 저장된 코스 목록
}
