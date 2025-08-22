package com.hackerton.hackerton2025.Service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor

public class OpenAiService {

    @Value("${gemini.apiKey}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${gemini.maxOutputTokens:512}")
    private int maxOutputTokens;

    private final RestClient rest = RestClient.create();

    /** 헬스체크: 모델 메타 조회 */
    public org.springframework.http.ResponseEntity<String> health() {
        String url = baseUrl + "/models/" + model + "?key=" + apiKey;
        return rest.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class);
    }

    /** AskRequest(question, system) → Gemini generateContent → 답 텍스트 반환 */
    public String ask(String question, String system) {
        String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;

        // request body (v1beta)
        // system이 있으면 systemInstruction 사용
        var body = new java.util.LinkedHashMap<String, Object>();

        // contents (user 메시지)
        var contents = List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", question))
        ));
        body.put("contents", contents);

        if (system != null && !system.isBlank()) {
            body.put("systemInstruction", Map.of(
                    "role", "system",
                    "parts", List.of(Map.of("text", system))
            ));
        }

        // generationConfig (선택)
        body.put("generationConfig", Map.of(
                "maxOutputTokens", maxOutputTokens,
                "temperature", 0.7
        ));

        try {
            JsonNode root = rest.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            // candidates[0].content.parts[0].text 추출
            String text = "";
            if (root != null) {
                JsonNode first = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                text = first.isMissingNode() ? "" : first.asText("");
            }
            if (text == null || text.isBlank()) {
                return String.valueOf(root); // 최소한 전체 원문 반환
            }
            return text;

        } catch (HttpClientErrorException e) {
            // Gemini 에러 메시지 드러내기
            String payload = e.getResponseBodyAsString();
            throw new RuntimeException("Gemini error " + e.getStatusCode() + ": " + payload, e);
        } catch (Exception e) {
            throw new RuntimeException("Gemini call failed: " + e.getMessage(), e);
        }
    }
}
