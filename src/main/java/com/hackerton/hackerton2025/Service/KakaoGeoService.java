package com.hackerton.hackerton2025.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
public class KakaoGeoService {

    @Value("${kakao.api.key:}")
    private String KAKAO_REST_KEY;   // application.properties에 kakao.api.key=... 넣어둔 값

    private static final String BASE = "https://dapi.kakao.com/v2/local/search/address.json";
    private static final ObjectMapper OM = new ObjectMapper();
    private final RestTemplate rest = new RestTemplate();

    /** 주소 문자열 → (lat,lng). 성공 시 Optional.of, 실패 시 Optional.empty */
    @Cacheable(cacheNames = "kakao:geocode", key = "#address")
    public Optional<LatLng> geocode(String address) {
        if (KAKAO_REST_KEY == null || KAKAO_REST_KEY.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "kakao api key is not configured");
        }
        if (address == null || address.isBlank()) return Optional.empty();

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(BASE)
                    .queryParam("query", address)
                    .build()
                    .encode(StandardCharsets.UTF_8) // ✅ 한글 주소 인코딩
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + KAKAO_REST_KEY.trim());
            headers.set("Accept", "application/json");

            ResponseEntity<String> res = rest.exchange(
                    new RequestEntity<Void>(headers, HttpMethod.GET, uri),
                    String.class
            );

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                log.warn("Kakao geocode non-2xx: status={}, body={}", res.getStatusCode(), res.getBody());
                return Optional.empty();
            }

            JsonNode docs = OM.readTree(res.getBody()).path("documents");
            if (!docs.isArray() || docs.isEmpty()) return Optional.empty();

            JsonNode d = docs.get(0);
            double lng = d.path("x").asDouble(); // 경도
            double lat = d.path("y").asDouble(); // 위도
            return Optional.of(new LatLng(lat, lng));

        } catch (Exception e) {
            log.error("Kakao geocode error: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public record LatLng(double lat, double lng) {}
}
