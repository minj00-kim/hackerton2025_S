// src/main/java/com/hackerton/hackerton2025/Config/CacheConfig.java
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

        // ----- Kakao -----
        var groupCount   = new CaffeineCache("kakao:groupCount",
                Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10_000).build());
        var keywordCount = new CaffeineCache("kakao:keywordCount",
                Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10_000).build());
        var categoryDocs = new CaffeineCache("kakao:categoryDocs",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build());
        var keywordDocs  = new CaffeineCache("kakao:keywordDocs",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build());
        var geocode      = new CaffeineCache("kakao:geocode",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).maximumSize(50_000).build());
        var region       = new CaffeineCache("kakao:region",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).maximumSize(100_000).build());
        var coord2region = new CaffeineCache("kakao:coord2region",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).maximumSize(100_000).build());

        // ----- Region summary -----
        var chungnamSggSummary = new CaffeineCache("regions:chungnam:sggSummary",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(2_000).build());
        var sggSummary = new CaffeineCache("regions:sgg:summary",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(5_000).build());

        // ----- SBIZ(소진공) -----
        var sbizCountRadius = new CaffeineCache("sbiz:count:radius",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(10_000).build());
        var sbizLclsRadius  = new CaffeineCache("sbiz:lcls:radius",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(10_000).build());
        var sbizFpop        = new CaffeineCache("sbiz:fpop",
                Caffeine.newBuilder().expireAfterWrite(6, TimeUnit.HOURS).maximumSize(50_000).build());

        // ✅ LCLS 버킷(이번 에러의 주인공) + 여유로 MCLS/SCLS도 등록
        var sbizBucketsLcls = new CaffeineCache("sbiz:buckets:lcls",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(20_000).build());
        var sbizBucketsMcls = new CaffeineCache("sbiz:buckets:mcls",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(20_000).build());
        var sbizBucketsScls = new CaffeineCache("sbiz:buckets:scls",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(20_000).build());

        var mgr = new SimpleCacheManager();
        mgr.setCaches(List.of(
                // kakao
                groupCount, keywordCount, categoryDocs, keywordDocs, geocode,
                region, coord2region,
                // region summary
                chungnamSggSummary, sggSummary,
                // sbiz
                sbizCountRadius, sbizLclsRadius, sbizFpop,
                sbizBucketsLcls, sbizBucketsMcls, sbizBucketsScls
        ));
        return mgr;
    }
}
