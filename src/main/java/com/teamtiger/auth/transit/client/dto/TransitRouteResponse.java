// com/teamtiger/auth/transit/dto/TransitRouteResponse.java
package com.teamtiger.auth.transit.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data @AllArgsConstructor
public class TransitRouteResponse {
    private int totalTime;     // 분
    private int payment;       // 원
    private int transferCount; // 회
    private int totalWalk;     // m
    private List<TransitStep> steps;
}
