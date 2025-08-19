package com.hackerton.hackerton2025.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoRegionService {

    @Value("${kakao.api.key:}")
    private String kakaoKey;

    private static final ObjectMapper OM = new ObjectMapper();
    private final RestTemplate rt = new RestTemplate();

    /**
     * 응답 모델: 전체코드 + 시도/시군구/읍면동 코드와 이름
     * - 레코드 기본 접근자(sidoCode(), sigungu(), ...) + 표준 getter(getSido(), getSigungu(), ...) 둘 다 제공
     * - 이름 접근용 별칭(sido(), sigungu(), dong())도 함께 제공
     */
    public static record Region(
            String fullCode,             // 10자리 전체 코드
            String sidoCode,             // 앞 2자리
            String sggCode,              // 앞 5자리
            String dongCode,             // 10자리
            String sidoName,             // region_1depth_name
            String sigunguName,          // region_2depth_name
            String dongName              // region_3depth_name
    ) {
        // 표준 getter 스타일
        public String getSido()     { return sidoName; }
        public String getSigungu()  { return sigunguName; }
        public String getDong()     { return dongName; }
        public String getSidoCode() { return sidoCode; }
        public String getSggCode()  { return sggCode; }
        public String getDongCode() { return dongCode; }

        // 이름 접근을 record 스타일 별칭으로도 제공
        public String sido()    { return sidoName; }
        public String sigungu() { return sigunguName; }
        public String dong()    { return dongName; }
    }

    /**
     * 좌표 → 행정코드 (행정동(H) 우선, 없으면 첫 문서)
     * 캐시 키는 lat/lng 5자리까지 반올림해서 사용(대략 ~1.1m 격자).
     */
    @Cacheable(
            cacheNames = "kakao:region",
            key = "T(java.lang.String).format('%.5f,%.5f', #lat, #lng)"
    )
    public Optional<Region> coord2region(double lat, double lng) {
        if (kakaoKey == null || kakaoKey.isBlank()) {
            log.error("Kakao REST API key not configured");
            return Optional.empty();
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/geo/coord2regioncode.json")
                    .queryParam("x", lng) // 경도
                    .queryParam("y", lat) // 위도
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            HttpHeaders h = new HttpHeaders();
            h.set("Authorization", "KakaoAK " + kakaoKey.trim());
            h.set("Accept", "application/json");

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, new HttpEntity<>(h), String.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                log.warn("coord2region non-2xx: {}", res.getStatusCode());
                return Optional.empty();
            }

            JsonNode docs = OM.readTree(res.getBody()).path("documents");
            if (!docs.isArray() || docs.isEmpty()) return Optional.empty();

            // H(행정동) 먼저, 없으면 첫 번째
            JsonNode pick = null;
            for (JsonNode d : docs) {
                if ("H".equals(d.path("region_type").asText())) { pick = d; break; }
            }
            if (pick == null) pick = docs.get(0);

            String code = pick.path("code").asText(""); // 10자리
            if (code.length() < 10) return Optional.empty();

            String sidoCode = code.substring(0, 2);
            String sggCode  = code.substring(0, 5);
            String dongCode = code.substring(0,10);

            String n1 = pick.path("region_1depth_name").asText("");
            String n2 = pick.path("region_2depth_name").asText("");
            String n3 = pick.path("region_3depth_name").asText("");

            return Optional.of(new Region(code, sidoCode, sggCode, dongCode, n1, n2, n3));

        } catch (RestClientResponseException e) {
            log.error("coord2region HTTP {}: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("coord2region error: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
