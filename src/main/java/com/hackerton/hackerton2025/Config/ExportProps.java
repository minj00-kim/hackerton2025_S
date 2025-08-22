package com.hackerton.hackerton2025.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component // 이 클래스를 빈으로 등록 (또는 @ConfigurationPropertiesScan 사용)
@ConfigurationProperties(prefix = "app.exports")
public class ExportProps {

    /** 크롤 결과 기본 저장 경로 (기본값) */
    private String crawlDir = "data/crawl/outputs";

    public String getCrawlDir() { return crawlDir; }
    public void setCrawlDir(String crawlDir) { this.crawlDir = crawlDir; }
}
