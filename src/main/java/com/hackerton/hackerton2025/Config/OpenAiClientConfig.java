package com.hackerton.hackerton2025.Config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class OpenAiClientConfig {
    @Bean
    public RestTemplate openAiRestTemplate(
            RestTemplateBuilder builder,
            @Value("${ai.service.base-url}") String baseUrl,
            @Value("${openai.timeout.millis:15000}") int timeoutMillis
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);

        return builder
                .rootUri(baseUrl) // https://api.openai.com/v1
                .requestFactory(() -> factory)
                .setConnectTimeout(Duration.ofMillis(timeoutMillis))
                .setReadTimeout(Duration.ofMillis(timeoutMillis))
                .build();
    }
}
