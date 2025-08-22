package com.hackerton.hackerton2025.Service;


import com.hackerton.hackerton2025.Dto.ArticleDto;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;


@Service

public class CrawlService {



    private final JsonStoreService store;

    public CrawlService(JsonStoreService store) {
        this.store = store;
    }

    /** 최신 디렉터리의 youth_grants_extracted.json을 읽어 ArticleDto로 변환 */
    public List<ArticleDto> loadLatestArticles(Path crawlBase) throws Exception {
        var latestDir = store.findLatestDir(crawlBase);
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");
        var rawList   = store.readArrayJson(jsonPath); // List<Map<String,Object>>
        return rawList.stream().map(this::toArticle).collect(Collectors.toList());
    }

    /** 키워드(여러 개 가능)로 제목/요약/태그 필터, 정렬 후 limit */
    public List<ArticleDto> filterArticles(List<ArticleDto> src, List<String> keywords, int limit) {
        if (keywords == null || keywords.isEmpty()) {
            return sortArticles(src).stream().limit(limit).toList();
        }
        var lowered = keywords.stream().filter(Objects::nonNull).map(String::trim).map(String::toLowerCase).toList();
        var filtered = src.stream().filter(a -> matches(a, lowered)).toList();
        return sortArticles(filtered).stream().limit(limit).toList();
    }

    /** 기본 정렬: score desc -> publishedAt desc -> title asc */
    private List<ArticleDto> sortArticles(List<ArticleDto> list) {
        return list.stream()
                .sorted(Comparator
                        .comparing((ArticleDto a) -> a.score() != null ? a.score() : -1.0).reversed()
                        .thenComparing(a -> a.publishedAt() != null ? a.publishedAt() : OffsetDateTime.MIN, Comparator.reverseOrder())
                        .thenComparing(a -> a.title() != null ? a.title() : ""))
                .toList();
    }

    private boolean matches(ArticleDto a, List<String> lowered) {
        var hay = new StringBuilder();
        if (a.title() != null) hay.append(a.title()).append(' ');
        if (a.summary() != null) hay.append(a.summary()).append(' ');
        if (a.tags() != null) hay.append(String.join(" ", a.tags())).append(' ');
        var s = hay.toString().toLowerCase();
        for (var kw : lowered) {
            if (kw.isEmpty()) continue;
            if (s.contains(kw)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private ArticleDto toArticle(Map<String, Object> m) {
        String title   = getStr(m, "title", "headline");
        String url     = getStr(m, "url", "link");
        String source  = getStr(m, "source", "site", "publisher");
        String summary = getStr(m, "summary", "content", "snippet", "description");

        OffsetDateTime publishedAt = parseDate(getStr(m, "publishedAt", "date", "published"));
        Double score = getDbl(m, "score", "rank", "priority");

        List<String> tags = getStrList(m, "tags", "keywords");

        return new ArticleDto(title, url, source, publishedAt, summary, score, tags);
    }

    private String getStr(Map<String, Object> m, String... keys) {
        for (var k : keys) {
            Object v = m.get(k);
            if (v instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    private Double getDbl(Map<String, Object> m, String... keys) {
        for (var k : keys) {
            Object v = m.get(k);
            if (v instanceof Number n) return n.doubleValue();
            if (v instanceof String s) {
                try { return Double.parseDouble(s); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStrList(Map<String, Object> m, String... keys) {
        for (var k : keys) {
            Object v = m.get(k);
            if (v instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof String s) out.add(s);
                }
                if (!out.isEmpty()) return out;
            }
            if (v instanceof String s && !s.isBlank()) {
                // 콤마/공백 분리
                return Arrays.stream(s.split("[,\\s]+")).filter(t->!t.isBlank()).toList();
            }
        }
        return List.of();
    }

    private OffsetDateTime parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return OffsetDateTime.parse(s); }
        catch (DateTimeParseException e) { return null; }
    }
}
