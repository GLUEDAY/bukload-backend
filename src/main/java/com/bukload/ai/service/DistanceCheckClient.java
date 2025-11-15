package com.bukload.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ADMIN 전용 /internal/distance-check 와의 연동을 추상화한 Stub.
 * 실제 구현 시 WebClient 로 내부 서비스 호출하여 구간거리/시간 계산.
 */
@Component
@RequiredArgsConstructor
public class DistanceCheckClient {

    public double calculateTotalDistanceKm(List<double[]> path){
        // TODO: 실제 내부 API 호출로 대체
        // 간단한 유클리드 근사 (시演용)
        double total = 0;
        for(int i=1;i<path.size();i++){
            double dx = path.get(i)[0] - path.get(i-1)[0];
            double dy = path.get(i)[1] - path.get(i-1)[1];
            total += Math.sqrt(dx*dx + dy*dy) * 111; // 위경도 대략 km 환산
        }
        return Math.round(total * 10.0) / 10.0;
    }

    public int estimateMinutes(double totalDistanceKm){
        // 대략 자동차 30km/h 기준
        return (int)Math.round((totalDistanceKm / 30.0) * 60.0);
    }
}
