// src/main/java/com/hackerton/hackerton2025/Service/SbizOpenApiService.java
package com.hackerton.hackerton2025.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SbizOpenApiService {

    private static final ObjectMapper OM = new ObjectMapper();

    @Value("${sbiz.apiKey:}")
    private String apiKey;

    // ✔ 실제 공개 베이스 URL
    @Value("${sbiz.baseUrl:https://apis.data.go.kr/B553077/api/open/sdsc2}")
    private String baseUrl;

    private final RestTemplate rt = new RestTemplate();

    private JsonNode call(URI uri) {
        try {
            ResponseEntity<String> res = rt.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                return OM.createObjectNode();
            }
            return OM.readTree(res.getBody());
        } catch (Exception e) {
            return OM.createObjectNode();
        }
    }

    /** 반경 내 총 점포 수 (totalCount만 조회) */
    @Cacheable(
            cacheNames = "sbiz:count:radius",
            key = "T(java.lang.String).format('%.5f,%.5f,%d', #lat, #lon, #radiusM)"
    )
    public int countInRadius(double lat, double lon, int radiusM) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/storeListInRadius")
                .queryParam("serviceKey", apiKey)   // 인코딩된 serviceKey 그대로
                .queryParam("type", "json")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1)        // 개수만 필요
                .queryParam("radius", radiusM)
                .queryParam("cx", lon)             // 경도
                .queryParam("cy", lat)             // 위도
                .build(true)                       // 인코딩 유지
                .toUri();

        return call(uri).path("body").path("totalCount").asInt(0);
    }

    /** 반경 내 업종 대분류(indsLclsNm) 버킷 집계 (최대 maxPages*1000건) */
    @Cacheable(
            cacheNames = "sbiz:buckets:lcls",
            key = "T(java.lang.String).format('%.5f,%.5f,%d,%d', #lat, #lon, #radiusM, #maxPages)"
    )
    public Map<String, Long> bucketsByLclsInRadius(double lat, double lon, int radiusM, int maxPages) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        for (int page = 1; page <= Math.max(1, maxPages); page++) {
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/storeListInRadius")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("type", "json")
                    .queryParam("pageNo", page)
                    .queryParam("numOfRows", 1000)
                    .queryParam("radius", radiusM)
                    .queryParam("cx", lon)
                    .queryParam("cy", lat)
                    .build(true)
                    .toUri();

            JsonNode items = call(uri).path("body").path("items");
            if (!items.isArray() || items.size() == 0) break;

            for (JsonNode it : items) {
                String lcls = it.path("indsLclsNm").asText(""); // 대분류명
                if (!lcls.isBlank()) buckets.merge(lcls, 1L, Long::sum);
            }
        }
        return buckets;
    }

    /** 법정동(10자리) 내 점포 수 */
    @Cacheable(cacheNames = "sbiz:count:dong", key = "#dongCode")
    public int countInDong(String dongCode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/storeListInDong")
                .queryParam("serviceKey", apiKey)
                .queryParam("type", "json")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1)
                .queryParam("divId", "adongCd")
                .queryParam("key", dongCode)
                .build(true)
                .toUri();

        return call(uri).path("body").path("totalCount").asInt(0);
    }
}
