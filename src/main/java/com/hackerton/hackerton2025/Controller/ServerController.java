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
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/server")
@Slf4j
@RequiredArgsConstructor
public class ServerController {

    // 값이 비어 있어도 부팅되게 기본값("") 허용
    @Value("${naver.api.id:}")
    private String NAVER_API_ID;

    @Value("${naver.api.secret:}")
    private String NAVER_API_SECRET;

    private static final String NAVER_OPENAPI = "https://openapi.naver.com";
    private static final ObjectMapper OM = new ObjectMapper();

    // 경로 파라미터
    @GetMapping("/naver/{name}")
    public List<Map<String, String>> naver(@PathVariable String name) {
        return searchLocation(name);
    }

    // 쿼리 파라미터
    @GetMapping("/naver")
    public List<Map<String, String>> naverSearchDynamic(@RequestParam String query) {
        return searchLocation(query);
    }

    // 네이버 지역 검색 API 호출
    private List<Map<String, String>> searchLocation(String query) {
        if (NAVER_API_ID.isBlank() || NAVER_API_SECRET.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "naver api key is not configured");
        }

        try {
            // 한글/공백 안전하게 인코딩
            URI uri = UriComponentsBuilder
                    .fromUriString(NAVER_OPENAPI)
                    .path("/v1/search/local.json")
                    .queryParam("query", query)
                    .queryParam("display", 10)
                    .queryParam("start", 1)
                    .queryParam("sort", "random")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            RestTemplate restTemplate = new RestTemplate();
            RequestEntity<Void> req = RequestEntity.get(uri)
                    .header("X-Naver-Client-Id", NAVER_API_ID)
                    .header("X-Naver-Client-Secret", NAVER_API_SECRET)
                    .build();

            ResponseEntity<String> res = restTemplate.exchange(req, String.class);

            if (!res.getStatusCode().is2xxSuccessful()) {
                // 어디서 막히는지 로그로 남김
                log.error("NAVER API non-2xx: status={}, body={}", res.getStatusCode(), res.getBody());
                int code = res.getStatusCode().value();
                if (code == 401 || code == 403) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Naver Client ID/Secret 오류 또는 '검색>지역' 권한 미설정");
                } else if (code == 429) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Naver API 호출 한도 초과");
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "naver api error: " + res.getStatusCode());
                }
            }

            JsonNode items = OM.readTree(res.getBody()).path("items");

            List<Map<String, String>> out = new ArrayList<>();
            for (JsonNode n : items) {
                Map<String, String> m = new HashMap<>();
                m.put("title", stripTags(n.path("title").asText()));   // <b> 태그 제거
                m.put("address", n.path("address").asText());
                m.put("roadAddress", n.path("roadAddress").asText(""));
                // 주의: mapx/mapy는 TM128 좌표 (위도/경도 아님)
                m.put("x", n.path("mapx").asText());
                m.put("y", n.path("mapy").asText());
                out.add(m);
            }
            return out;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("네이버 API 호출 중 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "naver api call failed");
        }
    }

    private static String stripTags(String s) {
        return s == null ? "" : s.replaceAll("<.*?>", "");
    }
}