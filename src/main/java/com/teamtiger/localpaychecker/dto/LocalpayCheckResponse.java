package com.teamtiger.localpaychecker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalpayCheckResponse {

    // 지역화폐 사용 가능 가맹점이 있는지 여부
    private boolean localpayUsable;

    // 매칭된 가맹점 리스트 (간단 요약)
    private List<MerchantInfo> matches;

    // 원래 요청 정보
    private String requestedSigunNm;
    private String requestedName;
    private String requestedRoadAddr;
}
