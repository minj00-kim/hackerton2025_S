package com.hackerton.hackerton2025.Service.Client;

import com.hackerton.hackerton2025.Dto.SearchDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor


public class SearchClient {


    private final RestClient rest = RestClient.create();

    @Value("${app.search.provider:bing}")
    private String provider;

    @Value("${app.search.bing.endpoint:https://api.bing.microsoft.com/v7.0/search}")
    private String bingEndpoint;

    private String bingKey() { return System.getenv("BING_SEARCH_KEY"); }

    public SearchResponse search(String query, int count) {
        if (!"bing".equalsIgnoreCase(provider)) {
            // 필요하면 google/serpapi 분기 추가
            throw new IllegalStateException("현재 provider=bing 만 지원");
        }
        try {
            String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create(bingEndpoint + "?q=" + q + "&count=" + Math.max(1, Math.min(count, 10)));

            var root = rest.get()
                    .uri(uri)
                    .header("Ocp-Apim-Subscription-Key", bingKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(com.fasterxml.jackson.databind.JsonNode.class);

            List<SearchItem> items = new ArrayList<>();
            if (root != null && root.has("webPages")) {
                for (var v : root.path("webPages").path("value")) {
                    items.add(SearchItem.builder()
                            .title(v.path("name").asText(""))
                            .url(v.path("url").asText(""))
                            .snippet(v.path("snippet").asText(""))
                            .build());
                }
            }
            return SearchResponse.builder().provider("bing").items(items).build();
        } catch (Exception e) {
            throw new RuntimeException("검색 실패: " + e.getMessage(), e);
        }
    }




}
