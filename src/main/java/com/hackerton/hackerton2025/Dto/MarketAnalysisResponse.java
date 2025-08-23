// src/main/java/com/hackerton/hackerton2025/Dto/MarketAnalysisResponse.java
package com.hackerton.hackerton2025.Dto;

import java.util.List;
import java.util.Map;

public record MarketAnalysisResponse(
        double lat, double lon, int radius,
        List<Recommendation> recommendations,
        Indicators indicators,
        List<AnalysisItem> analyses,

        // ✅ 프론트 표시용 필드
        String regionLabel,                // 예: "강남구 청담동"
        String summary,                    // 핵심 요약 한 문장
        SalesForecast salesForecast,       // 매출 전망(원 단위)
        List<HotArea> hotAreas,            // 유망 구역
        List<String> insights              // 불릿 인사이트
) {
    public record Recommendation(String category, double score, String reason) {}

    public record Indicators(
            Map<String, Long> countsByGroup,
            double densityPerKm2,
            double diversity,
            List<String> anchors
    ) {}

    public enum AnalysisStatus { ok, missing_data, unsupported }

    public record AnalysisItem(
            String type, AnalysisStatus status, double score, String summary, Map<String,Object> metrics
    ) {}

    // ✅ 새 구조체들
    public record SalesForecast(long expectedMonthly, long breakEvenMonthly) {}
    public record HotArea(String name, int score) {}
}
