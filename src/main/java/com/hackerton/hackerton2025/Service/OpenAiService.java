// src/main/java/com/hackerton/hackerton2025/Service/OpenAiService.java
package com.hackerton.hackerton2025.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper OM = new ObjectMapper();

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

    /** 일반 텍스트 응답 */
    public String ask(String question, String system) {
        String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;

        var body = new java.util.LinkedHashMap<String, Object>();

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

            String text = "";
            if (root != null) {
                JsonNode first = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                text = first.isMissingNode() ? "" : first.asText("");
            }
            if (text == null || text.isBlank()) {
                return String.valueOf(root);
            }
            return text;

        } catch (HttpClientErrorException e) {
            String payload = e.getResponseBodyAsString();
            throw new RuntimeException("Gemini error " + e.getStatusCode() + ": " + payload, e);
        } catch (Exception e) {
            throw new RuntimeException("Gemini call failed: " + e.getMessage(), e);
        }
    }

    /** JSON 모드 응답 (response_mime_type=application/json) */
// OpenAiService.java 안
    public JsonNode askJson(String system, String user) {
        String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;

        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user))
        )));
        if (system != null && !system.isBlank()) {
            body.put("systemInstruction", Map.of(
                    "role", "system",
                    "parts", List.of(Map.of("text", system))
            ));
        }
        // 👉 토큰 상향 + JSON 모드
        body.put("generationConfig", Map.of(
                "maxOutputTokens", Math.max(maxOutputTokens, 2048),
                "temperature", 0.2,
                "response_mime_type", "application/json"
        ));

        JsonNode root = rest.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        // candidates[0].content.parts[0].text 가 JSON 문자열
        String json = "";
        if (root != null) {
            JsonNode node = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            json = node.isMissingNode() ? "" : node.asText("");
        }
        if (json == null || json.isBlank()) {
            throw new RuntimeException("Gemini empty JSON response: " + root);
        }

        // 1차 파싱 시도
        try {
            return OM.readTree(json);
        } catch (Exception first) {
            // 👉 코드펜스/여분 텍스트 제거 + 중괄호/대괄호 균형 맞춰 재시도
            String repaired = extractJsonBlock(json);
            try {
                return OM.readTree(repaired);
            } catch (Exception second) {
                throw new RuntimeException("Gemini JSON call failed: " + second.getMessage(), second);
            }
        }
    }

    /** ```json … ``` 제거하고, 첫 { 또는 [ 부터 균형 맞는 지점까지 잘라서 반환 */
    private static String extractJsonBlock(String s) {
        if (s == null) return "";
        // 코드펜스 제거
        s = s.replace("```json", "```").replace("```JSON", "```");
        int a = s.indexOf("```");
        if (a >= 0) {
            int b = s.indexOf("```", a + 3);
            if (b > a) s = s.substring(a + 3, b).trim();
        }
        // 첫 { 또는 [ 찾기
        int i = s.indexOf('{');
        int j = s.indexOf('[');
        int start = (i >= 0 && (j < 0 || i < j)) ? i : j;
        if (start < 0) return s.trim();
        char open = s.charAt(start);
        char close = (open == '{') ? '}' : ']';

        int level = 0;
        for (int k = start; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == open) level++;
            else if (c == close) {
                level--;
                if (level == 0) return s.substring(start, k + 1).trim();
            }
        }
        // 닫힘이 없으면 남은 부분 반환(최대한)
        return s.substring(start).trim();
    }

}
