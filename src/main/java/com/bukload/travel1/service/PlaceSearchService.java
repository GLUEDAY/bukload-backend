package com.bukload.travel1.service;

import com.bukload.course.dto.PlaceDto;
import com.bukload.travel1.dto.PlaceResponse;
import java.util.List;

public interface PlaceSearchService {

    // 단일 검색
    PlaceResponse searchOne(String query);

    // 🔥 리스트 검색 추가
    List<PlaceDto> searchList(String query);
}
