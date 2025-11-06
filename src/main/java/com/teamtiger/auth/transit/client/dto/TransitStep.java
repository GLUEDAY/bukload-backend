// com/teamtiger/auth/transit/dto/TransitStep.java
package com.teamtiger.auth.transit.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class TransitStep {
    // type: WALK / BUS / SUBWAY
    private String type;
    private String line;        // 버스번호 또는 지하철 노선명 (도보는 null)
    private String startName;   // 승차/출발 지점
    private String endName;     // 하차/도착 지점
    private Integer stationCount; // 정차 수(도보 null)
    private int minutes;        // 소요시간(분)
}
