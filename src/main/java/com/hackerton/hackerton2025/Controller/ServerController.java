package com.hackerton.hackerton2025.Controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/server")
@Slf4j
public class ServerController {

    @Value("${naver.api.id}")
    private String NAVER_API_ID;

    @Value("${naver.api.secret}")
    private String NAVER_API_SECRET;

    // 경로 기반 검색
    @GetMapping("/naver/{name}")
    public List<Map<String, String>> naver(@PathVariable String name) {
        return searchLocation(name);
    }

    // 쿼리 파라미터 기반 검색
    @GetMapping("/naver")
    public List<Map<String, String>> naverSearchDynamic(@RequestParam String query) {
        return searchLocation(query);
    }

    // 네이버 지역 검색 API 호출
    private List<Map<String, String>> searchLocation(String query) {
        List<Map<String, String>> locations = new ArrayList<>();

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://openapi.naver.com")
                    .path("/v1/search/local.json")
                    .queryParam("query", query)
                    .queryParam("display", 10)
                    .queryParam("start", 1)
                    .queryParam("sort", "random")
                    .build()
                    .encode()
                    .toUri();

            RestTemplate restTemplate = new RestTemplate();
            RequestEntity<Void> req = RequestEntity.get(uri)
                    .header("X-Naver-Client-Id", NAVER_API_ID)
                    .header("X-Naver-Client-Secret", NAVER_API_SECRET)
                    .build();

            ResponseEntity<String> response = restTemplate.exchange(req, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode itemsNode = rootNode.path("items");

            for (JsonNode itemNode : itemsNode) {
                Map<String, String> location = new HashMap<>();
                location.put("title", itemNode.path("title").asText());
                location.put("address", itemNode.path("address").asText());
                location.put("latitude", itemNode.path("mapy").asText());
                location.put("longitude", itemNode.path("mapx").asText());

                locations.add(location);
            }

        } catch (Exception e) {
            log.error("네이버 API 호출 중 오류 발생: {}", e.getMessage());
        }

        return locations;
    }
}
