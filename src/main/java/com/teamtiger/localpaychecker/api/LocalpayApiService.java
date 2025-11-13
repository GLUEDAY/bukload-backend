package com.teamtiger.localpaychecker.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamtiger.localpaychecker.dto.MerchantInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LocalpayApiService {

    @Value("${localpay.api.base-url}")
    private String baseUrl;

    @Value("${localpay.api.key}")
    private String apiKey;

    @Value("${localpay.api.default-page-size:1000}")
    private int defaultPageSize;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 시군명 + 상호명으로 지역화폐 가맹점 조회
     */
    public Mono<List<MerchantInfo>> searchMerchants(String sigunNm,
                                                    String cmpnmNm,
                                                    String roadAddr) {

        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .queryParam("KEY", apiKey)
                .queryParam("Type", "json")
                .queryParam("pIndex", 1)
                .queryParam("pSize", defaultPageSize)
                .queryParam("SIGUN_NM", sigunNm)
                .queryParam("CMPNM_NM", cmpnmNm)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        log.info("[LOCALPAY] Call GG API: {}", uri);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> {   // flatMap 말고 map 써도 됨
                    try {
                        if (body == null || body.isBlank()) {
                            log.warn("[LOCALPAY] API body is null/blank");
                            return Collections.<MerchantInfo>emptyList();
                        }

                        log.debug("[LOCALPAY] API body = {}", body);

                        JsonNode root = objectMapper.readTree(body);

                        JsonNode serviceArray = root.get("RegionMnyFacltStus");
                        if (serviceArray == null || !serviceArray.isArray() || serviceArray.size() < 2) {
                            log.warn("[LOCALPAY] RegionMnyFacltStus 구조 이상: {}", root);
                            return Collections.<MerchantInfo>emptyList();
                        }

                        JsonNode rowNode = serviceArray.get(1).get("row");
                        if (rowNode == null || !rowNode.isArray()) {
                            log.info("[LOCALPAY] row 노드가 없거나 배열이 아님: {}", serviceArray);
                            return Collections.<MerchantInfo>emptyList();
                        }

                        List<MerchantInfo> list = new ArrayList<>();

                        rowNode.forEach(n -> {
                            MerchantInfo m = MerchantInfo.builder()
                                    .sigunNm(n.path("SIGUN_NM").asText(null))
                                    .cmpnmNm(n.path("CMPNM_NM").asText(null))
                                    .indutypeNm(n.path("INDUTYPE_NM").asText(null))
                                    .refineRoadnmAddr(n.path("REFINE_ROADNM_ADDR").asText(null))
                                    .refineLotnoAddr(n.path("REFINE_LOTNO_ADDR").asText(null))
                                    .refineZipno(n.path("REFINE_ZIPNO").asText(null))
                                    .refineWgs84Lat(parseDoubleOrNull(n.get("REFINE_WGS84_LAT")))
                                    .refineWgs84Logt(parseDoubleOrNull(n.get("REFINE_WGS84_LOGT")))
                                    .bizregNo(n.path("BIZREGNO").asText(null))
                                    .indutypeCd(n.path("INDUTYPE_CD").asText(null))
                                    .frcsNo(n.path("FRCS_NO").asText(null))
                                    .leadTaxManState(n.path("LEAD_TAX_MAN_STATE").asText(null))
                                    .leadTaxManStateCd(n.path("LEAD_TAX_MAN_STATE_CD").asText(null))
                                    .clsBizDay(n.path("CLSBIZ_DAY").asText(null))
                                    .build();

                            list.add(m);
                        });

                        log.info("[LOCALPAY] parsed {} merchants from API", list.size());
                        return list;

                    } catch (Exception e) {
                        log.error("[LOCALPAY] Error while parsing GG API response", e);
                        return Collections.<MerchantInfo>emptyList();
                    }
                })
                .onErrorResume(e -> {
                    log.error("[LOCALPAY] Error while calling GG API", e);
                    return Mono.just(Collections.<MerchantInfo>emptyList());
                });
    }

    private Double parseDoubleOrNull(JsonNode node) {
        if (node == null || !node.isValueNode()) return null;
        try {
            return Double.parseDouble(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
