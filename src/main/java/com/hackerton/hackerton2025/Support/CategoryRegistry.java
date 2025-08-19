package com.hackerton.hackerton2025.Support;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import com.hackerton.hackerton2025.Support.CategoryRegistry;
import java.util.List;
import java.util.Map;

public final class CategoryRegistry {

    private CategoryRegistry() {}

    // ✔ 순서 유지 위해 List 사용
    public static final List<String> CATEGORIES = List.of(
            "카페/디저트",
            "식당",
            "주점/호프",
            "편의",
            "패션/액세서리",
            "뷰티/미용",
            "의료/약국",
            "문화/취미",
            "레저/스포츠",
            "사무/공유오피스",
            "숙박",
            "창고/물류",
            "팝업/쇼룸",
            "기타"
    );

    /** "주점/ 호프", "문화/ 취미" 같이 슬래시 주변 공백을 정규화 */
    public static String canonicalize(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s*/\\s*", "/");
    }

    public static boolean isAllowed(String s) {
        String t = canonicalize(s);
        return t != null && CATEGORIES.contains(t);
    }

}
