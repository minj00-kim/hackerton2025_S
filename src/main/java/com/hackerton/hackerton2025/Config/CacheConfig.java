package com.hackerton.hackerton2025.Config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Kakao 관련
        var groupCount   = new CaffeineCache("kakao:groupCount",
                Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10_000).build());
        var keywordCount = new CaffeineCache("kakao:keywordCount",
                Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10_000).build());
        var categoryDocs = new CaffeineCache("kakao:categoryDocs",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build());
        var keywordDocs  = new CaffeineCache("kakao:keywordDocs",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build());
        var geocode      = new CaffeineCache("kakao:geocode",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).maximumSize(50_000).build()); // 주소→좌표는 오래 캐시

        // ✅ 좌표→행정구역 캐시 (KakaoRegionService.coord2region)
        var region       = new CaffeineCache("kakao:region",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).maximumSize(100_000).build());
        // (옵션) 예전 이름을 쓰는 코드가 있을 수도 있어서 같이 등록
        var coord2region = new CaffeineCache("kakao:coord2region",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).maximumSize(100_000).build());

        // Region 집계 관련
        var chungnamSggSummary = new CaffeineCache("regions:chungnam:sggSummary",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(2_000).build());
        var sggSummary = new CaffeineCache("regions:sgg:summary",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(5_000).build());

        var mgr = new SimpleCacheManager();
        mgr.setCaches(List.of(
                groupCount, keywordCount, categoryDocs, keywordDocs, geocode,
                region, coord2region, // ← 둘 다 등록(서비스는 kakao:region을 사용)
                chungnamSggSummary, sggSummary
        ));
        return mgr;
    }
}
