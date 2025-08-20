package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.SearchDto.SearchItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    /** 모델명과 엔드포인트는 설정 파일에서 주입 */
    @Value("${openai.model:gpt-5}")
    private String model;

    @Value("${openai.endpoint:https://api.openai.com/v1/responses}")
    private String endpoint;

    /** 프로퍼티(우선) → 없으면 환경변수에서 읽기 */
    @Value("${openai.apiKey:}")
    private String apiKeyProp;

    private RestClient rest() {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000); // 10초
        f.setReadTimeout(60_000);    // 60초
        return RestClient.builder().requestFactory(f).build();
    }

    private String resolveApiKey() {
        if (apiKeyProp != null && !apiKeyProp.isBlank()) return apiKeyProp;
        String env = System.getenv("OPENAI_API_KEY");
        return (env != null) ? env : "";
    }

    public String answerWithContext(String question, List<SearchItem> sources) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("질문이 비어 있습니다.");
        }

        String apiKey = resolveApiKey();
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다. " +
                    "환경변수 OPENAI_API_KEY 또는 application.properties의 openai.apiKey를 설정하세요.");
        }

        // 검색 컨텍스트를 간단히 합치기 (너무 길면 잘라서 보냄)
        StringBuilder ctx = new StringBuilder();
        if (sources != null && !sources.isEmpty()) {
            ctx.append("다음은 웹검색 일부 결과입니다. 사실과 링크를 근거로 간결히 답하세요.\n");
            int idx = 1;
            for (SearchItem s : sources.stream().limit(5).toList()) {
                String title = ellipsis(s.getTitle(), 150);
                String url = ellipsis(s.getUrl(), 250);
                String snip = ellipsis(s.getSnippet(), 400);
                ctx.append("[%d] %s\nURL: %s\nSnippet: %s\n\n".formatted(idx++, title, url, snip));
            }
            ctx.append("답변 끝에 참고한 출처 번호를 대괄호로 표기하세요. 예: [1][3]\n");
        }

        // OpenAI Responses API 바디
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("input", List.of(
                Map.of("role", "system",
                        "content", "당신은 사실 확인을 중시하는 한국어 조수입니다. 모르면 모른다고 답하세요."),
                Map.of("role", "user",
                        "content", ctx.toString() + "질문: " + question)
        ));

        try {
            Map<?, ?> resp = rest().post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            // 1) output_text가 있으면 그대로 반환
            Object out = (resp != null ? resp.get("output_text") : null);
            if (out != null) return out.toString();

            // 2) outputs[0].content[0].text 경로 대체 파싱
            String fallback = digText(resp);
            if (fallback != null) return fallback;

            // 3) 그래도 없으면 원문 문자열ㅇ
            return String.valueOf(resp);

        } catch (RestClientResponseException e) {
            String err = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            throw new RuntimeException("OpenAI 호출 실패(" + e.getRawStatusCode() + "): " + err, e);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI 호출 실패: " + e.getMessage(), e);
        }
    }

    private static String ellipsis(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    @SuppressWarnings("unchecked")
    private static String digText(Map<?, ?> resp) {
        if (resp == null) return null;
        Object outputs = resp.get("outputs");
        if (outputs instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m1) {
                Object content = m1.get("content");
                if (content instanceof List<?> cList && !cList.isEmpty()) {
                    Object c0 = cList.get(0);
                    if (c0 instanceof Map<?, ?> m2) {
                        Object text = m2.get("text");
                        return (text != null) ? text.toString() : null;
                    }
                }
            }
        }
        return null;
    }
}
