package com.hackerton.hackerton2025.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackerton.hackerton2025.Dto.MarketAnalysisRequest;
import com.hackerton.hackerton2025.Dto.MarketAnalysisResponse;
import com.hackerton.hackerton2025.Service.KakaoGeoService.LatLng;
import com.hackerton.hackerton2025.Support.CategoryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MarketAnalysisService {
    private static final ObjectMapper OM = new ObjectMapper();

    private final KakaoPlacesService kakaoPlacesService;
    private final KakaoGeoService kakaoGeo;
    private final KakaoRegionService kakaoRegion;
    private final IndicatorService indicatorService;
    private final OpenAiService openAi;
    private final SbizOpenApiService sbizOpenApiService;
    // ▼ 인구(MOIS) 서비스 주입
    private final MoisPopulationService moisPopulationService;

    // 입력 업종 보정
    private static final Map<String, String> INPUT_ALIAS = Map.of(
            "마트", "소매/편의점",
            "편의점", "소매/편의점",
            "슈퍼", "소매/편의점",
            "슈퍼마켓", "소매/편의점"
    );

    // 핫플레이스 허용/차단 태그
    private static final Set<String> HOT_ALLOW = Set.of(
            "hypermarket", "transport_hub", "station", "gov", "market", "university", "town_center", "residential"
    );
    private static final Set<String> HOT_BAN = Set.of(
            "school", "kids", "kindergarten", "elementary",
            "church", "cemetery", "park_only", "convenience" // 편의점은 인사이트 용도만
    );

    // 로컬 시그널 수집: count + 샘플(최대3) — 하이퍼마켓/주거/편의점 필터 적용
    private Map<String, Object> collectLocalSignals(double lat, double lon, int radius, Map<String, List<String>> kwMap) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (var e : kwMap.entrySet()) {
            String label = e.getKey();
            int count = 0;
            List<String> samples = new ArrayList<>();

            for (String kw : e.getValue()) {
                var docs = kakaoPlacesService.fetchKeywordDocs(lat, lon, kw, radius);
                for (var d : docs) {
                    String nm = d.placeName();
                    String cat = d.categoryName();

                    // 라벨별 필터
                    if ("hypermarket".equals(label) && !looksLikeHypermarket(nm, cat)) continue;
                    if ("residential".equals(label) && !looksLikeApartment(nm, cat)) continue;
                    if ("convenience".equals(label) && !looksLikeConvenience(nm, cat)) continue;

                    count++;
                    if (samples.size() < 3 && nm != null && !nm.isBlank() && !samples.contains(nm)) {
                        samples.add(nm);
                    }
                }
                if (samples.size() >= 3) break;
            }

            out.put(label, Map.of("count", count, "samples", samples));
        }
        return out;
    }

    /**
     * 폼 요청 → 지표계산 → LLM(JSON) → 보강(휴리스틱) → 응답 DTO
     */
    public MarketAnalysisResponse analyze(MarketAnalysisRequest req) {
        final int radius = req.radius() == null ? 600 : req.radius();

        // 1) 좌표
        final double lat, lon;
        if (req.lat() != null && req.lon() != null) {
            lat = req.lat();
            lon = req.lon();
        } else {
            LatLng g = kakaoGeo.geocode(req.address())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "주소 지오코딩 실패"));
            lat = g.lat();
            lon = g.lng();
        }

        // 2) 내부 지표
        var indicators = indicatorService.compute(lat, lon, radius);

        boolean hasCampus = indicators.anchors().stream()
                .filter(Objects::nonNull)
                .anyMatch(n -> n.contains("대학"));

        // 3) 지역 라벨
        var regionOpt = kakaoRegion.coord2region(lat, lon);
        String regionLabel = regionOpt
                .map(r -> (r.sigungu() + " " + r.dong()).trim())
                .orElse(Optional.ofNullable(req.address()).orElse(""));
        String dongCode = regionOpt.map(KakaoRegionService.Region::dongCode).orElse(null);

        // 3-α) 외부 집계
        int sbizTotalStores = sbizOpenApiService.countInRadius(lat, lon, radius);
        Map<String, Long> sbizLclsBuckets = sbizOpenApiService.bucketsByLclsInRadius(lat, lon, radius, 6);
        Map<String, Long> sbizUiBuckets = toUiBuckets(sbizLclsBuckets);

        // 3-β) 타겟 시그널 수집 (편의점은 상시 포함)
        var kwMap = audienceKeywordsFor(req.targetAudience());
        var localSignals = collectLocalSignals(lat, lon, radius, kwMap);

        // 3-γ) 핫플 후보
        List<Map<String, Object>> hotCandidates = buildHotAreaCandidates(regionLabel, indicators.anchors(), localSignals);

        // 3-δ) 합성 밀도
        double mergedDensity = composeMergedDensity(
                indicators.countsByGroup().values().stream().mapToLong(Long::longValue).sum(),
                sbizTotalStores, radius);

        // 3-ε) 인구 스냅샷(MOIS) 가져오기(좌표/행정동 어느 쪽이든)
        Map<String, Object> mois = safeMoisSnapshot(lat, lon, radius, dongCode);

        // 3-ζ) 타깃-환경 적합도 벡터/불일치 탐지(인구 반영)
        Map<String, Object> fit = computeAudienceFit(req.targetAudience(), localSignals, mois);

        // 4) 허용 업종
        List<String> allowedCategories =
                Optional.ofNullable(req.interestedCategories())
                        .filter(l -> !l.isEmpty())
                        .map(l -> l.stream()
                                .map(s -> INPUT_ALIAS.getOrDefault(s, s))
                                .map(CategoryRegistry::canonicalize)
                                .map(MarketAnalysisService::normalizeUiCategory)
                                .filter(CategoryRegistry::isAllowed)
                                .toList())
                        .orElseGet(() -> CategoryRegistry.CATEGORIES.stream()
                                .map(MarketAnalysisService::normalizeUiCategory)
                                .distinct()
                                .toList());

        List<String> allowedNoEtc = allowedCategories.stream()
                .filter(c -> !"기타".equals(c))
                .distinct()
                .toList();
        if (allowedNoEtc.isEmpty()) {
            allowedNoEtc = CategoryRegistry.CATEGORIES.stream()
                    .map(MarketAnalysisService::normalizeUiCategory)
                    .filter(c -> !"기타".equals(c))
                    .distinct()
                    .toList();
        }

        // 5) LLM 컨텍스트
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("address", req.address());
        ctx.put("region_label", regionLabel);
        ctx.put("lat", lat);
        ctx.put("lon", lon);
        ctx.put("radius_m", radius);
        ctx.put("analysis_types", Optional.ofNullable(req.analysisTypes()).orElse(List.of()));
        ctx.put("interested_categories", Optional.ofNullable(req.interestedCategories()).orElse(List.of()));
        ctx.put("allowed_categories", allowedNoEtc);
        ctx.put("budget_bracket", req.budgetBracket());
        ctx.put("experience", req.experience());
        ctx.put("target_audience", Optional.ofNullable(req.targetAudience()).orElse(""));

        ctx.put("indicators", Map.of(
                "countsByGroup", indicators.countsByGroup(),
                "densityPerKm2", indicators.densityPerKm2(),
                "diversity", indicators.diversity(),
                "anchors", indicators.anchors()
        ));
        ctx.put("external", Map.of(
                "sbiz", Map.of(
                        "totalStores", sbizTotalStores,
                        "lclsBuckets", sbizLclsBuckets,
                        "uiBuckets", sbizUiBuckets,
                        "dongCode", Optional.ofNullable(dongCode).orElse("")
                )
        ));
        ctx.put("flags", Map.of("hasCampus", hasCampus));
        ctx.put("local_signals", localSignals);
        ctx.put("hot_candidates", hotCandidates);
        ctx.put("meta", Map.of("mergedDensityPerKm2", mergedDensity));
        ctx.put("audience_fit", fit);
        ctx.put("mois_population", mois); // ★ 인구 스냅샷 컨텍스트 주입

        // 6) 프롬프트
        String system = PROMPT_SYSTEM;
        String user = "STRUCTURED_CONTEXT:\n" + toJson(ctx);

        // 7) LLM 호출
        JsonNode ai = openAi.askJson(system, user);

        // 8) 추천 파싱 및 필터 (+텍스트 한글화/키 제거)
        List<MarketAnalysisResponse.Recommendation> recs = new ArrayList<>();
        if (ai.path("recommendations").isArray()) {
            for (JsonNode r : ai.path("recommendations")) {
                recs.add(new MarketAnalysisResponse.Recommendation(
                        r.path("category").asText("추천"),
                        r.path("score").asDouble(0),
                        sanitizeText(r.path("reason").asText(""))
                ));
            }
        }
        Set<String> allowedSet = new LinkedHashSet<>(allowedNoEtc);
        recs = recs.stream()
                .map(r -> new MarketAnalysisResponse.Recommendation(
                        normalizeUiCategory(r.category()), clamp01(r.score()), sanitizeText(r.reason())))
                .filter(rc -> allowedSet.contains(rc.category()))
                .toList();

        // 백업 추천(인구 연령대 반영 가중)
        if (recs.isEmpty()) {
            String fallback = pickByCounts(indicators.countsByGroup(), allowedNoEtc);
            if (!sbizUiBuckets.isEmpty()) {
                String topUi = sbizUiBuckets.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey).orElse(null);
                if (topUi != null && allowedSet.contains(topUi)) fallback = topUi;
            }
            double popBias = populationDemandBias(fallback, mois);
            double baseScore = 0.45 * ((Number) fit.get("score")).doubleValue()
                    + 0.3 * scoreByDensity(mergedDensity)
                    + 0.15 * scoreByDiversity(indicators.diversity())
                    + 0.10 * popBias;
            recs = List.of(new MarketAnalysisResponse.Recommendation(
                    fallback, clamp01(baseScore), "상권 밀도·구성, 타깃 적합도와 주변 인구 특성을 반영한 기본 추천"
            ));
        }

        // 9) 분석 항목(선택한 것만) 파싱 + 보강(없으면 휴리스틱 생성)
        List<String> requested = Optional.ofNullable(req.analysisTypes()).orElse(List.of());
        Set<String> reqSet = new LinkedHashSet<>(requested);

        List<MarketAnalysisResponse.AnalysisItem> parsed = new ArrayList<>();
        if (ai.path("analyses").isArray()) {
            for (JsonNode a : ai.path("analyses")) {
                String type = a.path("type").asText("");
                if (!reqSet.contains(type)) continue; // 선택한 것만 유지
                Map<String, Object> metrics = OM.convertValue(a.path("metrics"), Map.class);
                parsed.add(new MarketAnalysisResponse.AnalysisItem(
                        type,
                        MarketAnalysisResponse.AnalysisStatus.ok,
                        clamp01(a.path("score").asDouble(0)),
                        sanitizeText(a.path("summary").asText("")),
                        metrics == null ? Map.of() : metrics
                ));
            }
        }

        // 휴리스틱으로 누락분 채우기(인구 반영)
        List<MarketAnalysisResponse.AnalysisItem> completed =
                completeRequestedAnalyses(parsed, reqSet, indicators, sbizUiBuckets, localSignals,
                        mergedDensity, fit, allowedNoEtc.get(0), recs.isEmpty() ? null : recs.get(0).category(), mois);

        // 10) 매출전망(없으면 휴리스틱 생성, 인구 반영)
        long expected = ai.path("salesForecast").path("expectedMonthly").asLong(0);
        long bep = ai.path("salesForecast").path("breakEvenMonthly").asLong(0);
        if (expected <= 0 || bep <= 0) {
            var est = estimateSales(mergedDensity, indicators.diversity(), sbizUiBuckets,
                    allowedNoEtc.get(0), ((Number) fit.get("score")).doubleValue(), mois);
            expected = est[0];
            bep = est[1];
        }
        var sales = new MarketAnalysisResponse.SalesForecast(expected, bep);

        // 11) 요약/핫플/인사이트
        String summary = sanitizeText(ai.path("summary").asText(""));
        if (summary == null || summary.isBlank()) {
            summary = synthesizeSummary(regionLabel, recs, mergedDensity, indicators, fit, localSignals, mois);
        }

        // 핫플: 후보 중에서만 선정 + 개별 편의점/소형 주거 제거 + 구역 라벨링 + 최대 2개
        List<MarketAnalysisResponse.HotArea> hotAreas = new ArrayList<>();
        List<String> priorityOrder = List.of("anchor", "university", "hypermarket", "station", "transport_hub", "gov", "market", "residential");
        hotCandidates.sort(Comparator.comparingInt(o -> {
            String src = String.valueOf(((Map<?, ?>) o).get("source"));
            int idx = priorityOrder.indexOf(src);
            return idx < 0 ? 999 : idx;
        }));
        LinkedHashSet<String> picked = new LinkedHashSet<>();
        for (var h : hotCandidates) {
            String nm = String.valueOf(h.get("name"));
            String src = String.valueOf(h.get("source"));
            if (!isHotAreaNameAllowed(nm, src)) continue;
            String zone = toZoneLabel(nm, src);
            if (picked.add(zone)) {
                hotAreas.add(new MarketAnalysisResponse.HotArea(zone, scoreHotAreaSource(src)));
            }
            if (hotAreas.size() >= 2) break;
        }
        if (hotAreas.isEmpty()) hotAreas.add(new MarketAnalysisResponse.HotArea(regionLabel + " 중심부", 60));

        // 인사이트: LLM 것이 밋밋하면 액션형으로 재작성(인구 반영)
        List<String> insights = new ArrayList<>();
        if (ai.path("insights").isArray()) {
            for (JsonNode n : ai.path("insights")) {
                String t = sanitizeText(n.asText(""));
                if (t != null && !t.isBlank()) insights.add(t);
            }
        }
        if (insights.isEmpty() || insights.stream().noneMatch(MarketAnalysisService::hasArrow)) {
            insights = actionableInsights(indicators, localSignals, sbizUiBuckets, fit,
                    mergedDensity, recs.isEmpty() ? allowedNoEtc.get(0) : recs.get(0).category(), mois);
        }

        // 12) 반환
        return new MarketAnalysisResponse(
                lat, lon, radius,
                recs, indicators, completed,
                regionLabel, summary, sales, hotAreas, insights
        );
    }

// ---------- 휴리스틱(보강) ----------

    private List<MarketAnalysisResponse.AnalysisItem> completeRequestedAnalyses(
            List<MarketAnalysisResponse.AnalysisItem> parsed,
            Set<String> requested,
            MarketAnalysisResponse.Indicators ind,
            Map<String, Long> sbizUiBuckets,
            Map<String, Object> localSignals,
            double mergedDensity,
            Map<String, Object> fit,
            String primaryAllowedCategory,
            String recommendedCategory,
            Map<String, Object> mois
    ) {
        Map<String, MarketAnalysisResponse.AnalysisItem> map = new LinkedHashMap<>();
        for (var a : parsed) map.putIfAbsent(a.type(), a);

        for (String t : requested) {
            if (map.containsKey(t)) continue;
            map.put(t, heuristicAnalysis(t, ind, sbizUiBuckets, localSignals, mergedDensity, fit,
                    primaryAllowedCategory, recommendedCategory, mois));
        }
        return new ArrayList<>(map.values());
    }

    private MarketAnalysisResponse.AnalysisItem heuristicAnalysis(
            String type,
            MarketAnalysisResponse.Indicators ind,
            Map<String, Long> sbizUiBuckets,
            Map<String, Object> localSignals,
            double mergedDensity,
            Map<String, Object> fit,
            String primaryAllowedCategory,
            String recommendedCategory,
            Map<String, Object> mois
    ) {
        double div = ind.diversity();
        double fitScore = ((Number) fit.get("score")).doubleValue();
        int anchors = ind.anchors() == null ? 0 : ind.anchors().size();

        // 인구 지표 추출
        double popDensity = getDouble(mois, "densityPerKm2", 0);
        double share20s = ageShare(mois, "20_29");
        double share30_59 = ageShare(mois, "30_39") + ageShare(mois, "40_49") + ageShare(mois, "50_59");
        double share60p = ageShare(mois, "60_69") + ageShare(mois, "70_plus");

        switch (type) {
            case "경쟁업체 분석": {
                double compLoad = competitorsFor(primaryAllowedCategory, sbizUiBuckets);
                double score = clamp01(0.55 - 0.25 * compLoad + 0.10 * scoreByDiversity(div) + 0.05 * populationDemandBias(primaryAllowedCategory, mois));
                String summary = String.format(
                        "반경 내 %s 분포, 업종 구성(다양도 %.2f), 앵커 %d곳과 인구 특성(30~59세 %.0f%%)을 고려했을 때 혼잡도는 보통 수준",
                        primaryAllowedCategory, div, anchors, share30_59 * 100.0
                );
                Map<String, Object> metrics = new LinkedHashMap<>();
                metrics.put("kakaoCounts", ind.countsByGroup());
                metrics.put("sbizUiBuckets", sbizUiBuckets);
                metrics.put("densityPerKm2", ind.densityPerKm2());
                metrics.put("diversity", div);
                metrics.put("anchors", ind.anchors());
                metrics.put("mergedDensityPerKm2", mergedDensity);
                metrics.put("mois", mois);
                return new MarketAnalysisResponse.AnalysisItem(type,
                        MarketAnalysisResponse.AnalysisStatus.ok, score, summary, metrics);
            }
            case "유동인구 분석": {
                int hub = cnt(localSignals, "transport_hub") + cnt(localSignals, "station");
                int parking = cnt(localSignals, "parking");
                double visitorIdx = visitorIndex(mergedDensity, hub, parking);
                double popAdj = Math.tanh((popDensity) / 50.0); // 0~1
                double score = clamp01(0.35 + 0.35 * visitorIdx + 0.15 * fitScore + 0.15 * popAdj);
                String summary = String.format(
                        "교통거점 %d곳, 주차 %d곳, 상권 밀도 %.1f/km², 인구밀도 %.1f/km²를 고려하면 유입 흐름은 보통 이상",
                        hub, parking, mergedDensity, popDensity
                );
                Map<String, Object> m = Map.of(
                        "hubs", hub, "parking", parking,
                        "mergedDensityPerKm2", mergedDensity,
                        "populationDensityPerKm2", popDensity
                );
                return new MarketAnalysisResponse.AnalysisItem(type,
                        MarketAnalysisResponse.AnalysisStatus.ok, score, summary, m);
            }
            case "입지 평가": {
                double targetAdj = (share30_59 > 0 ? 0.05 : 0) + (share20s > 0 ? 0.02 : 0);
                double score = clamp01(0.35 * scoreByDensity(mergedDensity) + 0.35 * fitScore + 0.20 * scoreByDiversity(div) + 0.10 * targetAdj);
                String summary = String.format(
                        "타깃 적합도 %.2f, 상권 밀도 %.1f/km², 구성 다양도 %.2f, 30~59세 비중 %.0f%%를 종합하면 입지는 보통 이상",
                        fitScore, mergedDensity, div, share30_59 * 100.0
                );
                Map<String, Object> m = Map.of(
                        "mergedDensityPerKm2", mergedDensity,
                        "fitScore", fitScore,
                        "diversity", div,
                        "populationShare30_59", share30_59
                );
                return new MarketAnalysisResponse.AnalysisItem(type,
                        MarketAnalysisResponse.AnalysisStatus.ok, score, summary, m);
            }
            case "상권 트렌드": {
                long totalUi = sbizUiBuckets.values().stream().mapToLong(Long::longValue).sum();
                double middleBias = share30_59;
                double score = clamp01(0.45 + 0.2 * scoreByDiversity(div) + 0.1 * Math.tanh(totalUi / 20.0) + 0.15 * middleBias);
                String summary = String.format(
                        "등록 사업체 %d곳, 구성 다양도 %.2f, 30~59세 비중 %.0f%% 기반으로 안정적 트렌드",
                        totalUi, div, share30_59 * 100.0
                );
                Map<String, Object> m = Map.of("diversity", div, "sbizTotal", totalUi, "populationShare30_59", share30_59);
                return new MarketAnalysisResponse.AnalysisItem(type,
                        MarketAnalysisResponse.AnalysisStatus.ok, score, summary, m);
            }
            case "리스크 분석": {
                double visitorIdx = visitorIndex(mergedDensity,
                        cnt(localSignals, "transport_hub") + cnt(localSignals, "station"),
                        cnt(localSignals, "parking"));
                // 점수는 '안전도'로 해석(높을수록 리스크 낮음)
                double seniorBias = share60p; // 시니어 비중 높으면 특정 업종에 불리할 수 있음
                double score = clamp01(0.20 + 0.35 * fitScore + 0.20 * visitorIdx + 0.15 * scoreByDiversity(div) - 0.10 * seniorBias);
                @SuppressWarnings("unchecked")
                List<String> mm = (List<String>) fit.get("mismatchReasons");
                String tail = (mm != null && !mm.isEmpty()) ? " / " + String.join(", ", mm) : "";
                String summary = "수요-환경 적합도, 접근성, 인구 구조를 반영한 위험도 평가" + tail;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("fitScore", fitScore);
                m.put("visitorIndex", visitorIdx);
                m.put("diversity", div);
                m.put("mergedDensityPerKm2", mergedDensity);
                m.put("mismatchReasons", mm);
                m.put("populationShare60_plus", share60p);
                return new MarketAnalysisResponse.AnalysisItem(type,
                        MarketAnalysisResponse.AnalysisStatus.ok, score, summary, m);
            }
            case "임대료 분석": {
                var rent = estimateRentKRW(mergedDensity);
                double score = clamp01(1.0 - rent.index); // 임대료 낮을수록 유리
                String summary = String.format("임대료 수준 %s(월 약 %,d원)으로 비용 부담은 %s",
                        rent.level, rent.monthlyKRW, rent.index < 0.45 ? "낮은 편" : "보통");
                Map<String, Object> m = Map.of("rentLevel", rent.level, "estimatedMonthlyRentKRW", rent.monthlyKRW, "rentIndex", rent.index);
                return new MarketAnalysisResponse.AnalysisItem(type,
                        MarketAnalysisResponse.AnalysisStatus.ok, score, summary, m);
            }
            case "고객 특성 분석": {
                @SuppressWarnings("unchecked")
                Map<String, Double> env = (Map<String, Double>) fit.get("envVector");
                String top = env.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("family");
                String topLabel = audienceLabel(top);
                double score = clamp01(((Number) fit.get("score")).doubleValue());
                String summary = String.format("주요 수요층은 %s 성향이 강함(20대 %.0f%%, 30~59세 %.0f%%, 60+ %.0f%%)",
                        topLabel, share20s * 100.0, share30_59 * 100.0, share60p * 100.0);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("envVector", env);
                m.put("fitScore", score);
                m.put("population", mois);
                return new MarketAnalysisResponse.AnalysisItem(type,
                        MarketAnalysisResponse.AnalysisStatus.ok, score, summary, m);
            }
            case "매출 예측": {
                long[] est = estimateSales(mergedDensity, div, sbizUiBuckets,
                        recommendedCategory != null ? recommendedCategory : primaryAllowedCategory, fitScore, mois);
                double score = clamp01(0.5 + 0.5 * fitScore);
                String summary = "카테고리 수요·경쟁·밀도·인구 지표를 반영한 월매출·손익분기점 산정";
                Map<String, Object> m = Map.of("expectedMonthly", est[0], "breakEvenMonthly", est[1], "population", mois);
                return new MarketAnalysisResponse.AnalysisItem(type,
                        MarketAnalysisResponse.AnalysisStatus.ok, score, summary, m);
            }
        }
        // 알 수 없는 타입은 기본 포맷
        return new MarketAnalysisResponse.AnalysisItem(
                type, MarketAnalysisResponse.AnalysisStatus.ok, 0.5, "주변 상권·인구·환경 지표를 반영한 요약", Map.of()
        );
    }

    private static class RentEst {
        String level;
        long monthlyKRW;
        double index;

        RentEst(String l, long k, double i) {
            level = l;
            monthlyKRW = k;
            index = i;
        }
    }

    private RentEst estimateRentKRW(double mergedDensity) {
        double idx = clamp01(mergedDensity / 35.0); // 0~1
        long monthly = Math.round(600_000 + idx * 1_800_000); // 0.6M ~ 2.4M
        String level = idx < 0.33 ? "낮음" : idx < 0.66 ? "보통" : "높음";
        return new RentEst(level, monthly, idx);
    }

    private long[] estimateSales(double mergedDensity, double diversity,
                                 Map<String, Long> sbizUiBuckets,
                                 String category, double fitScore,
                                 Map<String, Object> mois) {
        int compCount = (int) competitorsFor(category, sbizUiBuckets);
        double visitorIdx = visitorIndex(mergedDensity, 0, 0);

        // 전환율/객단가(간단 기준)
        double conv = switch (category) {
            case "카페/베이커리" -> 0.025;
            case "음식점/주점" -> 0.030;
            case "의료/약국" -> 0.018;
            case "교육/학원" -> 0.010;
            case "소매/편의점" -> 0.022;
            default -> 0.020;
        };
        int basket = switch (category) {
            case "카페/베이커리" -> 8_500;
            case "음식점/주점" -> 12_000;
            case "의료/약국" -> 18_000;
            case "교육/학원" -> 60_000;
            case "소매/편의점" -> 7_000;
            default -> 9_000;
        };

        // 인구 기반 수요 보정
        double share20s = ageShare(mois, "20_29");
        double share30_59 = ageShare(mois, "30_39") + ageShare(mois, "40_49") + ageShare(mois, "50_59");
        double share60p = ageShare(mois, "60_69") + ageShare(mois, "70_plus");
        double popDensity = getDouble(mois, "densityPerKm2", 0);

        double demandAdj = switch (category) {
            case "카페/베이커리" -> 0.9 + 0.4 * share20s + 0.2 * share30_59;
            case "음식점/주점" -> 0.9 + 0.3 * share30_59 + 0.1 * share20s + 0.05 * Math.min(1, popDensity / 60.0);
            case "의료/약국" -> 0.85 + 0.35 * share60p + 0.10 * share30_59;
            case "교육/학원" -> 0.85 + 0.35 * share20s + 0.10 * share30_59;
            case "소매/편의점" -> 0.9 + 0.2 * share20s + 0.2 * share30_59 + 0.05 * Math.min(1, popDensity / 60.0);
            default -> 0.9 + 0.2 * share30_59 + 0.1 * share20s;
        };
        demandAdj = clamp01(demandAdj); // 0~1

        // 월 방문자 근사: 상권밀도·타깃적합·인구보정
        double baseVisitors = 2500 * (0.5 + 0.5 * visitorIdx) * (0.55 + 0.45 * fitScore) * (0.7 + 0.6 * demandAdj);

        // 경쟁 보정(많을수록 하향)
        double compAdj = 1.0 / (1.0 + 0.12 * Math.max(0, compCount - 5));
        long transactions = Math.round(baseVisitors * conv * compAdj);
        long revenue = transactions * (long) basket;

        // 손익분기(고정비: 임대+인건비+기타 / 이익률)
        var rent = estimateRentKRW(mergedDensity);
        long staff = 1_800_000L;
        long other = 500_000L;
        double margin = switch (category) {
            case "카페/베이커리" -> 0.57;
            case "음식점/주점" -> 0.42;
            case "의료/약국" -> 0.35;
            case "교육/학원" -> 0.60;
            case "소매/편의점" -> 0.33;
            default -> 0.40;
        };
        long fixed = rent.monthlyKRW + staff + other;
        long bep = (long) Math.ceil(fixed / Math.max(0.05, margin));
        return new long[]{Math.max(1_000_000, revenue), Math.max(1_000_000, bep)};
    }

    private static String synthesizeSummary(String regionLabel,
                                            List<MarketAnalysisResponse.Recommendation> recs,
                                            double mergedDensity,
                                            MarketAnalysisResponse.Indicators ind,
                                            Map<String, Object> fit,
                                            Map<String, Object> localSignals,
                                            Map<String, Object> mois) {
        String cat = recs.isEmpty() ? "핵심 업종" : recs.get(0).category();
        double fitScore = ((Number) fit.get("score")).doubleValue();
        int hubs = cnt(localSignals, "transport_hub") + cnt(localSignals, "station");
        double popDensity = getDouble(mois, "densityPerKm2", 0);
        double mShare = ageShare(mois, "30_39") + ageShare(mois, "40_49") + ageShare(mois, "50_59");
        return String.format(
                "%s에서 %s 중심의 창업 적합도를 상권 밀도(%.1f/km²), 구성 다양도(%.2f), 교통거점 %d곳, 인구밀도 %.1f/km²·30~59세 비중 %.0f%%, 타깃 적합도(%.2f)로 요약했습니다.",
                regionLabel, cat, mergedDensity, ind.diversity(), hubs, popDensity, mShare * 100.0, fitScore
        );
    }

    // ===== 인사이트(액션형) 생성 =====
    private static List<String> actionableInsights(
            MarketAnalysisResponse.Indicators ind,
            Map<String, Object> localSignals,
            Map<String, Long> sbizUiBuckets,
            Map<String, Object> fit,
            double mergedDensity,
            String category,
            Map<String, Object> mois
    ) {
        List<String> out = new ArrayList<>();

        int convFromLocal = cnt(localSignals, "convenience");
        long convFromSbiz = sbizUiBuckets.getOrDefault("소매/편의점", 0L);
        long convTotal = Math.max(convFromLocal, convFromSbiz);
        if (convTotal >= 3) {
            out.add(String.format("편의점 %d곳 → 간편식·즉석식 수요 분산 가능성, %s는 빠른 회전/소용량 구색 유리",
                    convTotal, category));
        }

        int uni = cnt(localSignals, "university");
        if (uni > 0) {
            double fitScore = ((Number) fit.get("score")).doubleValue();
            out.add(String.format("대학교 %d곳(예: %s) 인접 → 평일 점심·시험기간 한시적 수요, 타깃 적합도(%.2f)에 맞춰 의존도 조절",
                    uni, joinSamples(localSignals, "university"), fitScore));
        }

        int res = cnt(localSignals, "residential");
        if (res > 0) {
            out.add(String.format("주거지 %d곳(예: %s) 밀집 → 저녁·주말 가족 단위 수요, 포장/배달 동선 최적화",
                    res, joinSamples(localSignals, "residential")));
        }

        int hubs = cnt(localSignals, "transport_hub") + cnt(localSignals, "station");
        int parking = cnt(localSignals, "parking");
        if (hubs + parking > 0) {
            out.add(String.format("교통거점 %d·주차 %d → 차량 접근성 양호, 전면주차/픽업존 설계 시 회전율 향상",
                    hubs, parking));
        }

        long food = ind.countsByGroup().getOrDefault("FOOD", 0L);
        if (food >= 15) {
            out.add(String.format("음식점 %d곳 → 카테고리 경쟁 강함, 런치 타임 특가·시그니처 메뉴로 차별화", food));
        }

        // 인구 기반 인사이트
        double popDensity = getDouble(mois, "densityPerKm2", 0);
        if (popDensity > 0) {
            out.add(String.format("인구밀도 %.1f명/km² → 회전율 전략 강화, 점심·퇴근 시간대 집중 운영", popDensity));
        }
        double share20s = ageShare(mois, "20_29");
        double share30_59 = ageShare(mois, "30_39") + ageShare(mois, "40_49") + ageShare(mois, "50_59");
        double share60p = ageShare(mois, "60_69") + ageShare(mois, "70_plus");
        if (share30_59 > 0) {
            out.add(String.format("30~59세 비중 %.0f%% → %s는 가족·직장인 선호 메뉴/가격대 설정", share30_59 * 100.0, category));
        } else if (share20s > 0) {
            out.add(String.format("20대 비중 %.0f%% → 외식·카페 수요 반영, 가격 민감도 고려", share20s * 100.0));
        } else if (share60p > 0) {
            out.add(String.format("60대 이상 비중 %.0f%% → 좌석 편의·대기시간 최소화, 순한 맛/건강 메뉴 강화", share60p * 100.0));
        }

        if (out.isEmpty()) {
            out.add(mergedDensity < 12 ?
                    "상권 밀도 낮음 → 임대·고정비 부담 낮고, 니즈 명확한 상품 유리" :
                    "상권 밀도 보통 이상 → 회전율 중심 운영 유리");
        }
        return out.subList(0, Math.min(out.size(), 6));
    }

    private static String joinSamples(Map<String, Object> ls, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) ls.getOrDefault(key, Map.of());
        @SuppressWarnings("unchecked")
        List<String> s = (List<String>) m.getOrDefault("samples", List.of());
        if (s.isEmpty()) return "";
        return String.join("/", s.subList(0, Math.min(2, s.size())));
    }

// ---------- HotArea 후보/필터/점수 & 합성밀도(오버로드) ----------

    private List<Map<String, Object>> buildHotAreaCandidates(String regionLabel,
                                                             List<String> anchors,
                                                             Map<String, Object> localSignals) {
        List<Map<String, Object>> out = new ArrayList<>();

        // 1) 앵커 기반
        if (anchors != null) {
            for (String nm : anchors) {
                if (nm == null || nm.isBlank()) continue;
                if (!isHotAreaNameAllowed(nm, "anchor")) continue;
                out.add(Map.of("name", nm, "source", "anchor"));
            }
        }

        // 2) 로컬 시그널 기반 (허용 태그만, 금지 태그 제외)
        for (var e : localSignals.entrySet()) {
            String tag = e.getKey();
            if (!HOT_ALLOW.contains(tag) || HOT_BAN.contains(tag)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> v = (Map<String, Object>) e.getValue();
            int c = ((Number) v.getOrDefault("count", 0)).intValue();
            @SuppressWarnings("unchecked")
            List<String> samples = (List<String>) v.getOrDefault("samples", List.of());
            if (c <= 0 || samples.isEmpty()) continue;

            for (String nm : samples) {
                if (!isHotAreaNameAllowed(nm, tag)) continue;
                out.add(Map.of("name", nm, "source", tag));
            }
        }

        if (out.isEmpty()) {
            out.add(Map.of("name", regionLabel + " 중심부", "source", "fallback"));
        }

        // 중복 제거(내용 동일 Map 기준)
        LinkedHashSet<Map<String, Object>> uniq = new LinkedHashSet<>(out);
        return new ArrayList<>(uniq);
    }

    private static boolean isHotAreaNameAllowed(String name, String source) {
        if (name == null) return false;
        String n = name.trim();
        String low = n.toLowerCase();

        // 금지: 학교/유치원/빌라/원룸/고시원/개별 편의점 등
        if (low.contains("유치원") || low.contains("초등") || low.contains("중학") || low.contains("고등")) return false;
        if (low.contains("빌라") || low.contains("원룸") || low.contains("고시원")) return false;
        if (looksLikeConvenience(n, null)) return false; // 편의점 단일 점포는 제외

        // 소스별 추가 검증
        if ("residential".equals(source)) {
            return looksLikeApartment(n, null); // 아파트/단지급만 허용
        }
        if ("hypermarket".equals(source)) {
            return looksLikeHypermarket(n, null); // 대형마트급만 허용
        }
        return true;
    }

    private static int scoreHotAreaSource(String src) {
        return switch (src) {
            case "anchor" -> 85;
            case "university" -> 80;
            case "hypermarket" -> 78;
            case "station", "transport_hub" -> 75;
            case "gov", "market" -> 70;
            case "residential" -> 65;
            default -> 60;
        };
    }

    // 합성 밀도 (long,int,int 호출도 안전하게 처리)
    private static double composeMergedDensity(long kakaoTotal, int sbizTotal, int radiusM) {
        return composeMergedDensity(kakaoTotal, (long) sbizTotal, radiusM);
    }

    private static double composeMergedDensity(long kakaoTotal, long sbizTotal, int radiusM) {
        double areaKm2 = Math.PI * Math.pow(radiusM / 1000.0, 2);
        long merged = Math.round((kakaoTotal + sbizTotal) / 2.0);
        return merged / Math.max(areaKm2, 1e-6);
    }

// ---------- 유틸 ----------

    private static String toJson(Object o) {
        try {
            return OM.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static boolean hasArrow(String s) {
        if (s == null) return false;
        return s.contains("→") || s.contains("->");
    }

    private static double clamp01(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }

    /**
     * 백업 업종 선택
     */
    private static String pickByCounts(Map<String, Long> counts, List<String> allowed) {
        String bestGroup = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("FOOD");

        String guess = switch (bestGroup) {
            case "CAFE" -> "카페/베이커리";
            case "FOOD" -> "음식점/주점";
            case "RETAIL" -> "소매/편의점";
            case "HEALTH" -> "의료/약국";
            case "EDU" -> "교육/학원";
            case "LEISURE" -> "레저/스포츠";
            case "LODGE" -> "숙박";
            case "FINANCE", "REAL_ESTATE" -> "부동산/금융";
            default -> "기타";
        };
        guess = normalizeUiCategory(guess);

        if (!allowed.isEmpty()) {
            if (allowed.contains(guess)) return guess;
            return allowed.get(0);
        }
        return guess;
    }

    // SBIZ LCLS → UI 버킷
    private static final Map<String, String> SBIZ_LCLS_TO_UI = Map.ofEntries(
            Map.entry("음식", "음식점/주점"),
            Map.entry("소매", "소매/편의점"),
            Map.entry("숙박", "숙박"),
            Map.entry("부동산", "부동산/금융"),
            Map.entry("금융·보험", "부동산/금융"),
            Map.entry("보건·사회복지", "의료/약국"),
            Map.entry("교육", "교육/학원"),
            Map.entry("예술·스포츠", "레저/스포츠"),
            Map.entry("운송·창고", "창고/물류"),
            Map.entry("수리·개인", "서비스업"),
            Map.entry("시설관리·임대", "서비스업"),
            Map.entry("과학·기술", "서비스업"),
            Map.entry("정보통신", "서비스업"),
            Map.entry("공공", "기타"),
            Map.entry("제조", "기타")
    );

    private static Map<String, Long> toUiBuckets(Map<String, Long> sbizLcls) {
        Map<String, Long> ui = new LinkedHashMap<>();
        if (sbizLcls == null) return ui;
        for (var e : sbizLcls.entrySet()) {
            String uiCat = SBIZ_LCLS_TO_UI.getOrDefault(e.getKey(), "기타");
            ui.merge(uiCat, e.getValue(), Long::sum);
        }
        return ui;
    }

    private static final Map<String, String> CATEGORY_ALIAS = Map.of(
            "카페/디저트", "카페/베이커리",
            "식당", "음식점/주점",
            "주점/호프", "음식점/주점",
            "뷰티/미용", "미용/헬스케어",
            "사무/공유오피스", "서비스업"
    );

    private static String normalizeUiCategory(String s) {
        if (s == null) return null;
        String t = CategoryRegistry.canonicalize(s);
        return CATEGORY_ALIAS.getOrDefault(t, t);
    }

    // ------------ 이름/카테고리 필터 & 라벨러 ------------
    private static boolean looksLikeConvenience(String nm, String cat) {
        String s = ((nm == null ? "" : nm) + " " + (cat == null ? "" : cat)).toLowerCase();
        return s.contains("편의점") || s.contains("gs25") || s.contains("세븐일레븐")
                || s.contains("이마트24") || s.contains("ministop") || s.contains("미니스톱") || s.matches(".*\\bcu\\b.*");
    }

    private static boolean looksLikeHypermarket(String nm, String cat) {
        String s = ((nm == null ? "" : nm) + " " + (cat == null ? "" : cat)).toLowerCase();
        if (looksLikeConvenience(nm, cat)) return false;
        return s.contains("대형마트") || s.contains("이마트 트레이더스") || s.matches(".*\\b이마트\\b.*")
                || s.contains("홈플러스") || s.contains("롯데마트") || s.contains("코스트코") || s.contains("트레이더스");
    }

    private static boolean looksLikeApartment(String nm, String cat) {
        String s = ((nm == null ? "" : nm) + " " + (cat == null ? "" : cat));
        return s.contains("아파트") || s.contains("주공") || s.contains("LH") || s.matches(".*\\d+단지.*");
    }

    private static String toZoneLabel(String name, String source) {
        if (name == null || name.isBlank()) return "";
        String n = name.trim();
        return switch (source) {
            case "university", "anchor" -> n + " 일대";
            case "station", "transport_hub" -> n + " 일대";
            case "hypermarket" -> n + " 상권 일대";
            case "gov" -> n + " 일대";
            case "residential" -> n.replace("아파트", "아파트 단지") + " 상가 일대";
            case "market" -> n + " 일대";
            default -> n + " 일대";
        };
    }

    // 타겟별 키워드
    private Map<String, List<String>> audienceKeywordsFor(String targetAudience) {
        String ta = Optional.ofNullable(targetAudience).orElse("").toLowerCase();

        Map<String, List<String>> kw = new LinkedHashMap<>();
        // 공통
        kw.put("hypermarket", List.of("이마트", "홈플러스", "롯데마트", "코스트코", "이마트 트레이더스"));
        kw.put("transport_hub", List.of("버스터미널", "환승센터", "시외버스", "기차역", "지하철역"));
        kw.put("gov", List.of("시청", "구청", "면사무소", "읍사무소", "주민센터"));
        // 편의점은 상시 수집(인사이트용)
        kw.put("convenience", List.of("편의점", "CU", "GS25", "세븐일레븐", "이마트24"));

        boolean student = ta.contains("학생") || ta.contains("대학생") || ta.contains("20");
        if (student) {
            kw.put("university", List.of("대학교", "대학", "캠퍼스"));
            kw.put("student_housing", List.of("기숙사", "원룸", "자취", "오피스텔"));
        }

        boolean office = ta.contains("직장인") || ta.contains("사무직") || ta.contains("회사원");
        if (office) {
            kw.put("workplace", List.of("산업단지", "공단", "테크노밸리", "오피스", "회사"));
            kw.put("parking", List.of("공영주차장", "주차타워"));
        }

        boolean familyMid = ta.contains("30") || ta.contains("40") || ta.contains("50") || ta.contains("가족");
        if (familyMid) {
            kw.put("residential", List.of("아파트", "주공", "LH", "주택단지"));
            kw.put("medical", List.of("병원", "내과", "정형외과", "소아과", "치과", "의원"));
            kw.put("park", List.of("체육공원", "근린공원", "어린이공원"));
            kw.put("school", List.of("초등학교", "중학교", "고등학교"));
            kw.put("kids", List.of("유치원", "어린이집", "키즈카페"));
        }

        boolean senior = ta.contains("시니어") || ta.contains("노년") || ta.contains("어르신") || ta.contains("60");
        if (senior) {
            kw.put("medical", List.of("병원", "내과", "정형외과", "재활병원", "한의원"));
            kw.put("senior_center", List.of("노인복지관", "경로당", "시니어센터"));
            kw.put("residential", List.of("아파트", "주택단지"));
            kw.put("park", List.of("체육공원", "산책로", "근린공원"));
        }

        boolean tourist = ta.contains("관광") || ta.contains("여행객") || ta.contains("관광객");
        if (tourist) {
            kw.put("lodging", List.of("호텔", "모텔", "펜션", "게스트하우스"));
            kw.put("tourist_spot", List.of("관광안내소", "관광명소", "문화재", "박물관"));
        }

        boolean commuter = ta.contains("통근") || ta.contains("출퇴근") || ta.contains("역세권");
        if (commuter) {
            kw.put("station", List.of("지하철역", "기차역", "KTX", "SRT"));
            kw.put("transport_hub", List.of("버스터미널", "환승센터"));
            kw.put("parking", List.of("공영주차장", "환승주차장"));
        }

        boolean kids = ta.contains("유아") || ta.contains("아이") || ta.contains("학부모");
        if (kids) {
            kw.put("kids", List.of("유치원", "어린이집", "키즈카페"));
            kw.put("school", List.of("초등학교"));
            kw.put("park", List.of("어린이공원", "놀이터"));
        }

        boolean night = ta.contains("야간") || ta.contains("야식") || ta.contains("밤");
        if (night) {
            kw.put("lodging", List.of("호텔", "모텔", "게스트하우스"));
            kw.put("transport_hub", List.of("버스터미널", "기차역"));
            kw.put("workplace", List.of("산업단지", "공단"));
            kw.put("convenience", List.of("편의점", "24시"));
        }

        boolean fitness = ta.contains("헬스") || ta.contains("운동") || ta.contains("피트니스") || ta.contains("요가") || ta.contains("필라테스");
        if (fitness) {
            kw.put("fitness", List.of("헬스장", "피트니스", "요가", "필라테스", "체육센터"));
            kw.put("park", List.of("체육공원", "조깅", "산책로"));
        }

        return kw;
    }

    // ==== 타깃-환경 적합도 계산(인구 반영 버전) ====
    private Map<String, Object> computeAudienceFit(String targetAudience, Map<String, Object> localSignals, Map<String, Object> mois) {
        Map<String, Double> tv = buildTargetVector(targetAudience);
        Map<String, Double> ev = buildEnvVector(localSignals);

        // 인구 구조를 환경벡터에 가산(가벼운 가중치)
        double s20 = ageShare(mois, "20_29");
        double s30 = ageShare(mois, "30_39");
        double s40 = ageShare(mois, "40_49");
        double s50 = ageShare(mois, "50_59");
        double s60 = ageShare(mois, "60_69");
        double s70 = ageShare(mois, "70_plus");

        ev.merge("student", s20 * 0.7, Double::sum);
        ev.merge("family", (s30 + s40 + s50) * 0.9, Double::sum);
        ev.merge("senior", (s60 + s70) * 1.0, Double::sum);
        // 정규화
        for (String k : ev.keySet()) ev.put(k, clamp01(ev.get(k)));

        double score = cosine(tv, ev);

        List<String> reasons = new ArrayList<>();
        addMismatchIf(tv, ev, "student", "대학생 타깃인데 대학·기숙사 시그널과 20대 비중이 낮음",
                localSignals, List.of("university", "student_housing"), reasons);
        addMismatchIf(tv, ev, "family", "30~50대 가족 타깃인데 아파트·의료·학교 시그널과 30~59세 비중이 약함",
                localSignals, List.of("residential", "medical", "school"), reasons);
        addMismatchIf(tv, ev, "senior", "시니어 타깃인데 의료·복지시설 시그널과 60+ 비중이 부족함",
                localSignals, List.of("medical", "senior_center"), reasons);
        addMismatchIf(tv, ev, "worker", "직장인 타깃인데 산업단지·오피스 시그널이 적음",
                localSignals, List.of("workplace"), reasons);
        addMismatchIf(tv, ev, "commuter", "통근층 타깃인데 환승센터·역세권 시그널이 약함",
                localSignals, List.of("transport_hub", "station", "parking"), reasons);
        addMismatchIf(tv, ev, "tourist", "관광객 타깃인데 숙박·관광명소 시그널이 적음",
                localSignals, List.of("lodging", "tourist_spot"), reasons);
        addMismatchIf(tv, ev, "kids", "유아·학부모 타깃인데 유치원·초등 시그널이 적음",
                localSignals, List.of("kids", "school"), reasons);
        addMismatchIf(tv, ev, "night", "야간 수요 타깃인데 심야 교통·숙박·24시 편의 시그널이 약함",
                localSignals, List.of("transport_hub", "lodging", "convenience"), reasons);
        addMismatchIf(tv, ev, "fitness", "운동 타깃인데 헬스·공원 시그널이 적음",
                localSignals, List.of("fitness", "park"), reasons);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("score", score);
        out.put("targetVector", tv);
        out.put("envVector", ev);
        out.put("mismatchReasons", reasons);
        return out;
    }

    // ==== 인구 스냅샷 취득(반경/행정동) – 리플렉션으로 메서드 유무 대응 ====
    private Map<String, Object> safeMoisSnapshot(double lat, double lon, int radius, String dongCode) {
        try {
            // 우선 좌표 기반 요약이 있으면 사용: summaryByCoord(double,double,int)
            try {
                var m = MoisPopulationService.class.getMethod("summaryByCoord", double.class, double.class, int.class);
                Object r = m.invoke(moisPopulationService, lat, lon, radius);
                if (r instanceof Optional<?> opt && opt.isPresent()) return (Map<String, Object>) opt.get();
            } catch (NoSuchMethodException ignore) {
            }

            // 없으면 행정동 기반: summaryByDong(String)
            if (dongCode != null) {
                try {
                    var m2 = MoisPopulationService.class.getMethod("summaryByDong", String.class);
                    Object r2 = m2.invoke(moisPopulationService, dongCode);
                    if (r2 instanceof Optional<?> opt2 && opt2.isPresent()) return (Map<String, Object>) opt2.get();
                } catch (NoSuchMethodException ignore) {
                }
            }
        } catch (Exception e) {
            // 무시하고 빈 맵
        }
        return Map.of(); // 인구 데이터 없을 때 빈 맵
    }

    // ==== 인구 → 업종별 수요 가중(0~1) ====
    private double populationDemandBias(String category, Map<String, Object> mois) {
        double s20 = ageShare(mois, "20_29");
        double s30_59 = ageShare(mois, "30_39") + ageShare(mois, "40_49") + ageShare(mois, "50_59");
        double s60p = ageShare(mois, "60_69") + ageShare(mois, "70_plus");
        return switch (category) {
            case "카페/베이커리" -> clamp01(0.4 * s20 + 0.3 * s30_59 + 0.1);
            case "음식점/주점" -> clamp01(0.35 * s30_59 + 0.2 * s20 + 0.1);
            case "의료/약국" -> clamp01(0.45 * s60p + 0.2 * s30_59 + 0.1);
            case "교육/학원" -> clamp01(0.45 * s20 + 0.2 * s30_59 + 0.1);
            case "소매/편의점" -> clamp01(0.25 * s20 + 0.25 * s30_59 + 0.1);
            default -> clamp01(0.2 * s30_59 + 0.1 * s20);
        };
    }

    // ==== 보조 유틸(인구/텍스트) ====
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        return v instanceof Map ? (Map<String, Object>) v : Map.of();
    }

    private static double getDouble(Map<String, Object> m, String k, double def) {
        Object v = m == null ? null : m.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return v == null ? def : Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    private static double ageShare(Map<String, Object> mois, String key) {
        Map<String, Object> ages = getMap(mois, "ageShares");
        return getDouble(ages, key, 0.0); // 0~1 비율 기대
    }

    // LLM이 만든 영문 키/내부 경로가 문장에 섞여 나오면 지워서 한글만 남김
    private static String sanitizeText(String s) {
        if (s == null) return "";
        String t = s;
        // 영어 키/경로 패턴 제거
        t = t.replaceAll("\\b(sbiz|audience_fit|hot_candidates|local_signals|meta)\\.[a-zA-Z0-9_\\.:-]+", "");
        t = t.replaceAll("\\([a-zA-Z0-9_\\.:-]+\\)", "");
        t = t.replaceAll("\\s{2,}", " ").trim();
        return t;
    }

    // ==== 기존 벡터/스코어 유틸 ====
    private static Map<String, Double> buildTargetVector(String taRaw) {
        String ta = Optional.ofNullable(taRaw).orElse("").toLowerCase();
        Map<String, Double> v = zeroVec();
        if (ta.contains("대학생") || ta.contains("학생") || ta.contains("20")) v.put("student", 1.0);
        if (ta.contains("30") || ta.contains("40") || ta.contains("50") || ta.contains("가족")) v.put("family", 1.0);
        if (ta.contains("60") || ta.contains("시니어") || ta.contains("노년") || ta.contains("어르신")) v.put("senior", 1.0);
        if (ta.contains("직장인") || ta.contains("사무") || ta.contains("회사원")) v.put("worker", 1.0);
        if (ta.contains("통근") || ta.contains("역세권")) v.put("commuter", 1.0);
        if (ta.contains("관광")) v.put("tourist", 1.0);
        if (ta.contains("유아") || ta.contains("학부모") || ta.contains("아이")) v.put("kids", 1.0);
        if (ta.contains("야간") || ta.contains("밤") || ta.contains("야식")) v.put("night", 1.0);
        if (ta.contains("헬스") || ta.contains("운동") || ta.contains("피트니스") || ta.contains("요가") || ta.contains("필라테스"))
            v.put("fitness", 1.0);
        return v;
    }

    private static Map<String, Double> buildEnvVector(Map<String, Object> ls) {
        Map<String, Double> v = zeroVec();
        int u = cnt(ls, "university") + cnt(ls, "student_housing");
        int fam = cnt(ls, "residential") + cnt(ls, "medical") + cnt(ls, "school") + cnt(ls, "park");
        int sen = cnt(ls, "senior_center") + cnt(ls, "medical");
        int work = cnt(ls, "workplace") + cnt(ls, "gov");
        int com = cnt(ls, "transport_hub") + cnt(ls, "station") + cnt(ls, "parking");
        int tour = cnt(ls, "lodging") + cnt(ls, "tourist_spot");
        int kid = cnt(ls, "kids") + cnt(ls, "school");
        int night = cnt(ls, "lodging") + cnt(ls, "transport_hub") + cnt(ls, "workplace") + cnt(ls, "convenience");
        int fit = cnt(ls, "fitness") + cnt(ls, "park");

        v.put("student", clamp01(u / 6.0));
        v.put("family", clamp01(fam / 12.0));
        v.put("senior", clamp01(sen / 8.0));
        v.put("worker", clamp01(work / 8.0));
        v.put("commuter", clamp01(com / 8.0));
        v.put("tourist", clamp01(tour / 6.0));
        v.put("kids", clamp01(kid / 8.0));
        v.put("night", clamp01(night / 10.0));
        v.put("fitness", clamp01(fit / 8.0));
        return v;
    }

    private static Map<String, Double> zeroVec() {
        Map<String, Double> v = new LinkedHashMap<>();
        for (String k : List.of("student", "family", "senior", "worker", "commuter", "tourist", "kids", "night", "fitness"))
            v.put(k, 0.0);
        return v;
    }

    private static int cnt(Map<String, Object> ls, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) ls.getOrDefault(key, Map.of());
        Object c = m.get("count");
        return c instanceof Number ? ((Number) c).intValue() : 0;
    }

    private static double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0, na = 0, nb = 0;
        for (String k : a.keySet()) {
            double x = a.get(k), y = b.getOrDefault(k, 0.0);
            dot += x * y;
            na += x * x;
            nb += y * y;
        }
        if (na == 0 && nb == 0) return 0.5;
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static void addMismatchIf(
            Map<String, Double> tv,
            Map<String, Double> ev,
            String key,
            String baseMsg,
            Map<String, Object> localSignals,
            List<String> evidenceKeys,
            List<String> outReasons
    ) {
        if (tv.getOrDefault(key, 0.0) >= 0.7 && ev.getOrDefault(key, 0.0) < 0.2) {
            String msg = baseMsg;
            List<String> bits = new ArrayList<>();
            for (String ek : evidenceKeys) {
                int c = cnt(localSignals, ek);
                if (c > 0) bits.add(ek + ":" + c);
            }
            if (!bits.isEmpty()) msg += " (" + String.join(", ", bits) + ")";
            outReasons.add(msg);
        }
    }

    private static double scoreByDensity(double mergedDensity) {
        if (mergedDensity < 6) return 0.35;
        if (mergedDensity < 12) return 0.5;
        if (mergedDensity < 20) return 0.6;
        return 0.7;
    }

    private static double scoreByDiversity(double H) {
        if (H < 1.0) return 0.4;
        if (H < 1.4) return 0.5;
        if (H < 1.8) return 0.6;
        return 0.7;
    }

    private static double competitorsFor(String category, Map<String, Long> sbizUiBuckets) {
        return switch (category) {
            case "카페/베이커리" -> sbizUiBuckets.getOrDefault("카페/베이커리", 0L);
            case "음식점/주점" -> sbizUiBuckets.getOrDefault("음식점/주점", 0L);
            case "의료/약국" -> sbizUiBuckets.getOrDefault("의료/약국", 0L);
            case "교육/학원" -> sbizUiBuckets.getOrDefault("교육/학원", 0L);
            case "소매/편의점" -> sbizUiBuckets.getOrDefault("소매/편의점", 0L);
            default -> sbizUiBuckets.values().stream().mapToLong(Long::longValue).sum();
        };
    }

    private static double visitorIndex(double mergedDensity, int hubs, int parking) {
        double d = Math.tanh(mergedDensity / 12.0); // 0~1
        double a = Math.tanh((hubs + 0.5 * parking) / 4.0);
        return clamp01(0.6 * d + 0.4 * a);
    }

    private static String audienceLabel(String key) {
        return switch (key) {
            case "student" -> "대학생/20대";
            case "family" -> "30~50대 가족";
            case "senior" -> "시니어";
            case "worker" -> "직장인";
            case "commuter" -> "통근층";
            case "tourist" -> "관광객";
            case "kids" -> "유아·학부모";
            case "night" -> "야간수요";
            case "fitness" -> "운동";
            default -> "주요 고객";
        };
    }

    // ===== 프롬프트 =====
// ===== 프롬프트 =====
    private static final String PROMPT_SYSTEM = """
            너는 한국의 상권·입지 분석가다. 입력 STRUCTURED_CONTEXT(JSON)만 근거로 응답한다.
            과장/권유성 표현 금지. 내부 키/경로 노출 금지. JSON만 반환.
            
            반드시 JSON 형식:
            {
            "summary": "string",
            "recommendations": [{"category":"string","score":0..1,"reason":"string"}],
            "salesForecast": {"expectedMonthly": 0, "breakEvenMonthly": 0},
            "hotAreas": [{"name":"string","score":0..100}],
            "insights": ["string", "..."],
            "analyses": [{"type":"string","status":"ok","score":0..1,"summary":"string","metrics":{}}]
            }
            
            중요 규칙
            
            선택한 분석유형만 반환한다: analysis_types 배열에 있는 타입만 analyses에 포함.
            
            데이터가 부족해도 'missing_data'를 쓰지 않는다. 주변 상권·인구·환경 지표(mergedDensityPerKm2, diversity, sbiz.uiBuckets, local_signals, anchors, mois_population)를 근거로 수치를 산정한다.
            
            '추정/예상/가정' 같은 단어는 쓰지 않는다. 결과는 간결한 수치+근거 문장으로 제시.
            
            타깃–환경 불일치를 반영한다: audience_fit.score와 mismatchReasons를 요약/리스크에 활용.
            
            recommendations[*].category는 allowed_categories 중 하나만 사용. reason에는 근거 수치(밀도, 다양도, 경쟁량, 인구비중, 타깃 적합도 등)를 포함.
            
            hotAreas는 hot_candidates에서만 최대 2개 선택하며, 개별 편의점·소형 점포는 제외하고 구역형 라벨(“…일대/…상가 일대”)로 표기한다.
            
            insights는 반드시 '근거 → 시사점' 한줄 요약으로 작성한다. 예)
            
            "편의점 6곳(샘플: CU OO점) → 간편식 수요 분산, 소용량/빠른회전 구색 유리"
            
            "대학교 1곳(한서대 서산캠퍼스) 인접 → 평일 점심 수요 존재, 가족 타깃은 의존도 낮게"
            
            영문 키/내부 경로(audience_fit.score 등)를 문장에 그대로 노출하지 말고 한국어로 요약한다.
            """;
}