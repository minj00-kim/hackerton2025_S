package com.hackerton.hackerton2025.Service.Client;

import com.fasterxml.jackson.databind.JsonNode;                 // ✅ 타입 명시
import com.hackerton.hackerton2025.Dto.SearchItem;            // ✅ DTO 임포트
import com.hackerton.hackerton2025.Dto.SearchResponse;        // ✅ DTO 임포트
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WebClient {

    private final RestClient rest = RestClient.create();

    @Value("${app.search.provider:bing}")
    private String provider;

    @Value("${app.search.bing.endpoint:https://api.bing.microsoft.com/v7.0/search}")
    private String bingEndpoint;

    // 프로퍼티로도 키를 받을 수 있게 하고(옵션), 없으면 환경변수로 대체
    @Value("${app.search.bing.key:}")
    private String bingKeyProp;

    private String bingKey() {
        String k = System.getenv("BING_SEARCH_KEY");
        if (k == null || k.isBlank()) k = bingKeyProp;
        if (k == null || k.isBlank()) {
            throw new IllegalStateException(
                    "Bing Search API Key 누락: 환경변수 BING_SEARCH_KEY 또는 app.search.bing.key 를 설정하세요."
            );
        }
        return k;
    }

    public SearchResponse search(String query, int count) {
        if (!"bing".equalsIgnoreCase(provider)) {
            // 필요하면 google/serpapi 등으로 분기 추가
            throw new IllegalStateException("현재 provider=bing 만 지원합니다. (app.search.provider=bing)");
        }
        try {
            String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
            int k = Math.max(1, Math.min(count, 10)); // 1~10 사이로 클램프
            URI uri = URI.create(bingEndpoint + "?q=" + q + "&count=" + k);

            JsonNode root = rest.get()
                    .uri(uri)
                    .header("Ocp-Apim-Subscription-Key", bingKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);

            List<SearchItem> items = new ArrayList<>();
            if (root != null && root.has("webPages")) {
                for (JsonNode v : root.path("webPages").path("value")) {
                    items.add(SearchItem.builder()
                            .title(v.path("name").asText(""))
                            .url(v.path("url").asText(""))
                            .snippet(v.path("snippet").asText(""))
                            .build());
                }
            }

            return SearchResponse.builder()
                    .provider("bing")
                    .items(items)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("검색 실패: " + e.getMessage(), e);
        }
    }
}
