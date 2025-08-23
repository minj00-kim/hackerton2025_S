package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.MarketAnalysisResponse;
import com.hackerton.hackerton2025.Support.KakaoCategoryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class IndicatorService {

    private final KakaoPlacesService kakaoPlaces;
    private final KakaoCategoryRegistry kakaoCats;

    /**
     * lat/lon/radius로 그룹별 카운트, 밀도, 다양성, 앵커(실명) 계산
     * - countsByGroup: 내부 그룹(FOOD/CAFE/RETAIL/...)별 개수
     * - anchors: MT1/SC4/SW8 같은 앵커 코드 기반 + '대학교/대학/캠퍼스/대학원' 키워드 폴백
     */
    public MarketAnalysisResponse.Indicators compute(double lat, double lon, int radiusM) {
        Map<String, Long> countsByGroup = new LinkedHashMap<>();
        Set<String> anchorNames = new LinkedHashSet<>();
        long total = 0;

        for (String code : kakaoCats.kakaoCodes()) {
            int c = kakaoPlaces.countByGroup(lat, lon, code, radiusM);
            countsByGroup.merge(kakaoCats.groupOf(code), (long) c, Long::sum);
            total += c;

            if (c > 0 && kakaoCats.isAnchor(code)) {
                var docs = kakaoPlaces.fetchCategoryDocs(lat, lon, code, radiusM);
                int picked = 0;
                for (var d : docs) {
                    String nm = d.placeName();
                    if (nm != null && !nm.isBlank()) {
                        anchorNames.add(nm);
                        if (++picked >= 5) break;
                    }
                }
            }
        }

        if (anchorNames.isEmpty()) {
            List<String> campusKeywords = List.of("대학교", "대학", "캠퍼스", "대학원");
            outer:
            for (String kw : campusKeywords) {
                var docs = kakaoPlaces.fetchKeywordDocs(lat, lon, kw, radiusM);
                for (var d : docs) {
                    String nm = d.placeName();
                    String cat = d.categoryName() == null ? "" : d.categoryName();
                    String blob = (nm == null ? "" : nm) + " " + cat;
                    if (blob.contains("대학")) {
                        if (nm != null && !nm.isBlank()) anchorNames.add(nm);
                        if (anchorNames.size() >= 5) break outer;
                    }
                }
            }
        }

        double areaKm2 = Math.PI * Math.pow(radiusM / 1000.0, 2);
        double density = total / Math.max(areaKm2, 1e-6);

        double H = 0.0;
        if (total > 0) {
            for (var e : countsByGroup.entrySet()) {
                double p = e.getValue() / (double) total;
                if (p > 0) H += -p * Math.log(p);
            }
        }

        return new MarketAnalysisResponse.Indicators(
                countsByGroup, density, H, new ArrayList<>(anchorNames)
        );
    }
}
