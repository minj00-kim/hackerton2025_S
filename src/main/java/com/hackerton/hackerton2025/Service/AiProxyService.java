package com.hackerton.hackerton2025.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AiProxyService {

    // RestTemplate 빈이 없어도 new 로 바로 사용 가능
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.node.url:https://api.openai.com}")
    private String base;

    public ResponseEntity<String> health() {
        String url = base + "/health";
        return restTemplate.getForEntity(url, String.class);
    }

    public ResponseEntity<String> geocode(String address) {
        String url = UriComponentsBuilder.fromHttpUrl(base + "/geocode")
                .queryParam("address", address)
                .toUriString();
        return restTemplate.getForEntity(url, String.class);
    }

    public ResponseEntity<String> recommend(String jsonBody) {
        String url = base + "/recommend";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(jsonBody, headers);
        return restTemplate.postForEntity(url, req, String.class);
    }

    public ResponseEntity<String> simulate(String jsonBody) {
        String url = base + "/simulate";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(jsonBody, headers);
        return restTemplate.postForEntity(url, req, String.class);
    }

    public ResponseEntity<String> compare(String a, String b) {
        String url = UriComponentsBuilder.fromHttpUrl(base + "/compare")
                .queryParam("a", a)
                .queryParam("b", b)
                .toUriString();
        return restTemplate.getForEntity(url, String.class);
    }
}