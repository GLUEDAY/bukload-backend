package com.bukload.localpaychecker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInfo {
    private String sigunNm;            // 시군명
    private String cmpnmNm;            // 상호명
    private String indutypeNm;         // 업종명
    private String refineRoadnmAddr;   // 도로명 주소
    private String refineLotnoAddr;    // 지번 주소
    private String refineZipno;        // 우편번호
    private Double refineWgs84Lat;     // 위도
    private Double refineWgs84Logt;    // 경도
    private String bizregNo;           // 사업자등록번호
    private String indutypeCd;         // 업종코드
    private String frcsNo;             // 가맹점번호
    private String leadTaxManState;    // 휴·폐업 상태
    private String leadTaxManStateCd;  // 휴·폐업 상태 코드
    private String clsBizDay;          // 폐업일자
}
