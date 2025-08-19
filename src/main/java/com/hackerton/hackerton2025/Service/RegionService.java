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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegionService {

    @Value("${kakao.api.key:}")
    private String kakaoKey;

    private static final ObjectMapper OM = new ObjectMapper();
    private final RestTemplate rt = new RestTemplate();

    /** 좌표 → 법정동(시/구/동 + 코드들) */
    @Cacheable(cacheNames = "kakao:region",
            key = "T(java.lang.String).format('%.5f:%.5f', #lat, #lng)")
    public Optional<RegionInfo> fromLatLng(double lat, double lng) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://dapi.kakao.com/v2/local/geo/coord2regioncode.json")
                .queryParam("x", lng)
                .queryParam("y", lat)
                .build().toUri();

        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "KakaoAK " + kakaoKey);
        h.set("Accept", "application/json");

        ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, new HttpEntity<>(h), String.class);
        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) return Optional.empty();

        try {
            for (JsonNode d : OM.readTree(res.getBody()).path("documents")) {
                if (!"B".equals(d.path("region_type").asText())) continue; // 법정동만
                String code = d.path("code").asText("");                   // 10자리
                if (code.length() < 10) continue;

                return Optional.of(new RegionInfo(
                        d.path("region_1depth_name").asText(""), // 시/도
                        d.path("region_2depth_name").asText(""), // 시군구
                        d.path("region_3depth_name").asText(""), // 동
                        code.substring(0, 2),                    // 시도코드 2
                        code.substring(0, 5),                    // 시군구 5
                        code.substring(0, 10)                    // 법정동 10
                ));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** 시/구/동 + 코드들 */
    public record RegionInfo(
            String sido, String sigungu, String dong,
            String sidoCode, String sggCode, String dongCode
    ) {}
}
