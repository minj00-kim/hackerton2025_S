package com.hackerton.hackerton2025.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5500","http://localhost:3000") // 프론트 주소로 교체
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)   // 쿠키 쓰면 꼭 필요(GuestCookieFilter 때문에)
                .maxAge(3600);
    }
}