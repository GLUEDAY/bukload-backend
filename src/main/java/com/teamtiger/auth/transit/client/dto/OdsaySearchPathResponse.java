// com/teamtiger/auth/transit/client/dto/OdsaySearchPathResponse.java
package com.teamtiger.auth.transit.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class OdsaySearchPathResponse {
    private Result result;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Path> path;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        private Info info;
        private List<SubPath> subPath;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private int totalTime;      // 총 소요시간(분)
        private int payment;        // 요금
        private int busTransitCount;
        private int subwayTransitCount;
        private int totalWalk;      // 총 도보(미터)
        private int transferCount;  // 환승 횟수
        private String mapObj;      // 경로 그리기용
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubPath {
        private int trafficType;      // 1=지하철, 2=버스, 3=도보
        private int sectionTime;      // 구간 소요시간(분)
        private Integer stationCount; // 정차역/정류장 수(도보면 null)
        private String startName;     // 승차 정류장/역
        private String endName;       // 하차 정류장/역
        private List<Lane> lane;      // 노선 정보(버스번호/호선명)
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Lane {
        private String name;   // 지하철 노선명
        private String busNo;  // 버스 번호
    }
}
