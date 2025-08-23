// src/main/java/com/hackerton/hackerton2025/Support/KakaoCategoryRegistry.java
package com.hackerton.hackerton2025.Support;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class KakaoCategoryRegistry {
    // kakao category_group_code -> 내부 그룹
    private static final Map<String, String> MAP = new LinkedHashMap<>();

    // 집객 앵커(대형마트/학교/지하철)
    private static final Set<String> ANCHORS = Set.of("MT1", "SC4", "SW8");

    static {
        // 리테일/편의
        put("MT1", "RETAIL");   // 대형마트
        put("CS2", "RETAIL");   // 편의점
        put("BK9", "FINANCE");  // 은행
        put("AG2", "REAL_ESTATE"); // 중개업소(부동산)
        put("PO3", "PUBLIC");   // 공공기관
        put("PK6", "PARKING");  // 주차장
        put("OL7", "GAS");      // 주유/충전

        // 외식/카페
        put("FD6", "FOOD");     // 음식점
        put("CE7", "CAFE");     // 카페

        // 교육
        put("PS3", "EDU");      // 어린이집/유치원
        put("SC4", "EDU");      // 학교
        put("AC5", "EDU");      // 학원

        // 의료/약국
        put("HP8", "HEALTH");   // 병원
        put("PM9", "HEALTH");   // 약국

        // 문화/관광/숙박/교통
        put("CT1", "LEISURE");  // 문화시설
        put("AT4", "LEISURE");  // 관광명소
        put("AD5", "LODGE");    // 숙박
        put("SW8", "TRANSIT");  // 지하철역
    }

    private static void put(String code, String group) { MAP.put(code, group); }

    public String groupOf(String kakaoCode) { return MAP.getOrDefault(kakaoCode, "ETC"); }
    public boolean isAnchor(String kakaoCode) { return ANCHORS.contains(kakaoCode); }
    public Set<String> kakaoCodes() { return MAP.keySet(); }
}
