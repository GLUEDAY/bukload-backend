package com.bukload.transit;

import com.bukload.transit.client.OdsayClient;
import com.bukload.transit.client.dto.OdsaySearchPathResponse;
import com.bukload.transit.client.dto.TransitRouteResponse;
import com.bukload.transit.client.dto.TransitStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class TransitService {

    private final OdsayClient odsayClient;

    public Mono<TransitRouteResponse> searchBestRoute(
            double sx, double sy, double ex, double ey, Integer searchPathType
    ) {
        return odsayClient.searchPath(sx, sy, ex, ey, searchPathType)
                .map(resp -> {
                    if (resp == null || resp.getResult() == null || resp.getResult().getPath() == null) {
                        throw new ResponseStatusException(NOT_FOUND, "경로를 찾지 못했습니다.");
                    }

                    List<TransitRouteResponse> routes = new ArrayList<>();

                    for (OdsaySearchPathResponse.Path p : resp.getResult().getPath()) {
                        var info = p.getInfo();
                        List<TransitStep> steps = new ArrayList<>();

                        if (p.getSubPath() != null) {
                            p.getSubPath().forEach(sp -> {
                                String type = switch (sp.getTrafficType()) {
                                    case 1 -> "SUBWAY";
                                    case 2 -> "BUS";
                                    case 3 -> "WALK";
                                    default -> "UNKNOWN";
                                };

                                String line = null;
                                if (sp.getLane() != null && !sp.getLane().isEmpty()) {
                                    var lane = sp.getLane().get(0);
                                    line = (lane.getBusNo() != null) ? lane.getBusNo() : lane.getName();
                                }

                                steps.add(new TransitStep(
                                        type,
                                        line,
                                        sp.getStartName(),
                                        sp.getEndName(),
                                        sp.getStationCount(),
                                        sp.getSectionTime()
                                ));
                            });
                        }

                        // 기존 transferCount 재계산(도보 제외)
                        int realTransfers = 0;
                        String lastLine = null;
                        for (TransitStep step : steps) {
                            if ("WALK".equals(step.getType())) continue;
                            if (lastLine != null && !lastLine.equals(step.getLine())) {
                                realTransfers++;
                            }
                            lastLine = step.getLine();
                        }

                        routes.add(new TransitRouteResponse(
                                info.getTotalTime(),
                                info.getPayment(),
                                realTransfers,
                                info.getTotalWalk(),
                                steps
                        ));
                    }

                    // --- 단일 최적 경로 선택 ---
                    Comparator<TransitRouteResponse> cmp =
                            Comparator.comparingInt(TransitRouteResponse::getTotalTime)         // 1) 최소 시간
                                    .thenComparingInt(TransitRouteResponse::getPayment)         // 2) 최소 요금 (도보=0원 우선)
                                    .thenComparingInt(TransitService::countNonWalkSegments)     // 3) 비도보(버스/지하철) 적을수록
                                    .thenComparingInt(TransitRouteResponse::getTransferCount)   // 4) 환승 적을수록
                                    .thenComparingInt(TransitRouteResponse::getTotalWalk);      // 5) 도보 총합 적을수록

                    return routes.stream()
                            .min(cmp)
                            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "경로를 찾지 못했습니다."));
                })
                .onErrorMap(e -> (e instanceof ResponseStatusException) ? e
                        : new ResponseStatusException(BAD_REQUEST, "ODsay 호출 실패", e));
    }

    private static int countNonWalkSegments(TransitRouteResponse r) {
        if (r.getSteps() == null) return 0;
        int cnt = 0;
        for (TransitStep s : r.getSteps()) {
            if (s != null && !"WALK".equals(s.getType())) cnt++;
        }
        return cnt;
    }
}

