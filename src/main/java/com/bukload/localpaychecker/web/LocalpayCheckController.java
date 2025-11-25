package com.bukload.localpaychecker.web;

import com.bukload.localpaychecker.api.LocalpayApiService;
import com.bukload.localpaychecker.dto.LocalpayCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LocalpayCheckController {

    private final LocalpayApiService apiService;

    /**
     * 지역화폐 사용 가능 여부 체크
     * <p>
     * 예)
     * GET /localpay/check?sigunNm=용인시&name=베이프하우스&roadAddr=상현로 126
     * <p>
     * - sigunNm, name은 공공데이터 API 요청에 사용
     * - roadAddr는 지금 단계에서는 "그냥 요청 정보 표시용"만 사용 (필터링 X)
     */
    @GetMapping("/localpay/check")
    public Mono<LocalpayCheckResponse> checkLocalpayUsable(
            @RequestParam String sigunNm,
            @RequestParam String name,
            @RequestParam(required = false) String roadAddr
    ) {
        return apiService.searchMerchants(sigunNm, name, roadAddr)
                .map(all -> {
                    boolean localpayUsable = !all.isEmpty();  // 👉 한 건이라도 있으면 true

                    return LocalpayCheckResponse.builder()
                            .localpayUsable(localpayUsable)
                            .matches(all)
                            .requestedSigunNm(sigunNm)
                            .requestedName(name)
                            .requestedRoadAddr(roadAddr)
                            .build();
                });
    }

    @GetMapping("/localpay/check/simple")
    public Mono<Boolean> checkSimple(
            @RequestParam String sigunNm,
            @RequestParam String name
    ) {
        return apiService.isLocalpayUsable(sigunNm, name);
    }


}