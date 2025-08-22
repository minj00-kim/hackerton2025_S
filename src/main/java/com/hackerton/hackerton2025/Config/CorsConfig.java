package com.hackerton.hackerton2025.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                        // 필요하면 사내 IP대역도: "http://192.168.*.*:*"
                )
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location","Content-Disposition","X-Total-Count") // 프론트에서 읽어야 하면
                .allowCredentials(true)   // 쿠키 전송 허용
                .maxAge(3600);
    }
}