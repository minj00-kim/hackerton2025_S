package com.hackerton.hackerton2025.Controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.hackerton.hackerton2025.Service.KakaoGeoService;

@RestController
@RequestMapping("/api/server")
@Slf4j
@RequiredArgsConstructor
public class ServerController {

    private final KakaoGeoService kakaoGeoService;

    @Value("${kakao.api.key:}")
    private String KAKAO_REST_KEY;

    private static final String KAKAO_BASE = "https://dapi.kakao.com";
    private static final ObjectMapper OM = new ObjectMapper();

    // Kakao 키워드 검색 (장소명 → 좌표/주소)
    @GetMapping("/kakao/{name}")
    public List<Map<String, String>> kakaoKeyword(@PathVariable String name) {
        return searchLocationKakao(name);
    }

    @GetMapping("/kakao")
    public List<Map<String, String>> kakaoKeywordQuery(@RequestParam String query) {
        return searchLocationKakao(query);
    }

    private List<Map<String, String>> searchLocationKakao(String query) {
        if (KAKAO_REST_KEY == null || KAKAO_REST_KEY.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "kakao api key is not configured");
        }
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(KAKAO_BASE)
                    .path("/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .queryParam("size", 10) // 1~15
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + KAKAO_REST_KEY.trim());
            headers.set("Accept", "application/json");

            ResponseEntity<String> res = rest.exchange(
                    new RequestEntity<Void>(headers, HttpMethod.GET, uri),
                    String.class
            );

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                int code = res.getStatusCode().value();
                log.error("KAKAO keyword non-2xx: status={}, body={}", res.getStatusCode(), res.getBody());
                if (code == 401 || code == 403) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kakao REST API 키 오류 또는 권한 미설정");
                if (code == 429) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Kakao API 호출 한도 초과");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "kakao api error: " + res.getStatusCode());
            }

            JsonNode docs = OM.readTree(res.getBody()).path("documents");
            List<Map<String, String>> out = new ArrayList<>();

            for (JsonNode d : docs) {
                // Kakao: x=lng(경도), y=lat(위도)
                String x = d.path("x").asText();
                String y = d.path("y").asText();

                Map<String, String> m = new HashMap<>();
                m.put("title", d.path("place_name").asText());
                m.put("address", d.path("address_name").asText(""));
                m.put("roadAddress", d.path("road_address_name").asText(""));
                m.put("x", x);
                m.put("y", y);
                m.put("lng", x);
                m.put("lat", y);
                out.add(m);
            }
            return out;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 키워드 검색 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "kakao api call failed");
        }
    }

    // Kakao 지오코딩 (주소 → 좌표)
    @GetMapping("/geocode")
    public Map<String, Object> geocode(@RequestParam String address) {
        var latLng = kakaoGeoService.geocode(address)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주소 결과 없음"));
        Map<String, Object> out = new HashMap<>();
        out.put("address", address);
        out.put("lat", latLng.lat());
        out.put("lng", latLng.lng());
        return out;
    }
}
