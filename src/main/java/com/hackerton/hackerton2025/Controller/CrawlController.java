package com.hackerton.hackerton2025.Controller;


import com.hackerton.hackerton2025.Service.JsonStoreService;
import com.hackerton.hackerton2025.Service.PythonRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/crawl")


public class CrawlController {

    private final PythonRunner py;
    private final JsonStoreService store;

    public CrawlController(PythonRunner py, JsonStoreService store) {
        this.py = py; this.store = store;
    }

    @Value("${app.exports.crawlDir}")
    private String crawlBase;

    // 파이썬 크롤러 실행 (node-ai/crawl_youth_grants.py)
    @PostMapping("/run")
    public ResponseEntity<?> runCrawl(
            @RequestParam(defaultValue = "청년창업 지원금") String query,
            @RequestParam(defaultValue = "40") int maxItems
    ) throws Exception {

        List<String> args = List.of(
                "crawl_youth_grants.py", "--query", query, "--max-items", String.valueOf(maxItems)
        );

        Map<String,String> env = new HashMap<>();
        String apiKey = System.getenv("OPENAI_API_KEY"); // 요약 쓰는 경우에만 필요
        if (apiKey != null) env.put("OPENAI_API_KEY", apiKey);

        var res = py.run(args.get(0), args.subList(1, args.size()), env, Duration.ofMinutes(6));
        if (res.exitCode() != 0) {
            return ResponseEntity.internalServerError().body(Map.of("stderr", res.stderr()));
        }

        var latestDir = store.findLatestDir(Path.of(crawlBase));
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");

        // ✅ 결과 파일 존재 체크
        if (!jsonPath.toFile().exists()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "RESULT_NOT_FOUND",
                    "dir", latestDir.toString(),
                    "expected", jsonPath.toString(),
                    "stdout", res.stdout(),
                    "stderr", res.stderr()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "dir", latestDir.toString(),
                "json", jsonPath.toString(),
                "stdout", res.stdout()
        ));
    }

    // 최신 결과 반환
    @GetMapping("/latest")
    public ResponseEntity<?> latest() throws Exception {
        var latestDir = store.findLatestDir(Path.of(crawlBase));
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");

        // ✅ 결과 파일 존재 체크
        if (!jsonPath.toFile().exists()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "RESULT_NOT_FOUND",
                    "dir", latestDir.toString(),
                    "expected", jsonPath.toString()
            ));
        }

        var data = store.readArrayJson(jsonPath);
        return ResponseEntity.ok(data);
    }

    // ✅ 최신 기사 리스트(정렬) 반환
    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(defaultValue = "20") int limit) throws Exception {
        var latestDir = store.findLatestDir(Path.of(crawlBase));
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");
        if (!jsonPath.toFile().exists()) {
            return ResponseEntity.status(404).body(Map.of("error","RESULT_NOT_FOUND","expected", jsonPath.toString()));
        }

        var arr = store.readArrayJson(jsonPath); // List<Map<String,Object>>
        arr.sort(CrawlController::compareArticleLikeMaps);

        if (limit < 1) limit = 1;
        return ResponseEntity.ok(arr.stream().limit(limit).toList());
    }

    // ✅ 키워드 검색: ?q=창업&limit=20  또는 ?q=창업,대출
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(name = "q") String q,
            @RequestParam(defaultValue = "20") int limit
    ) throws Exception {
        var latestDir = store.findLatestDir(Path.of(crawlBase));
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");
        if (!jsonPath.toFile().exists()) {
            return ResponseEntity.status(404).body(Map.of("error","RESULT_NOT_FOUND","expected", jsonPath.toString()));
        }

        var arr = store.readArrayJson(jsonPath); // List<Map<String,Object>>
        var kws = Arrays.stream(q.split("[,\\s]+"))
                .map(String::trim).filter(s -> !s.isBlank())
                .map(String::toLowerCase).toList();

        var filtered = arr.stream().filter(m -> {
            var title   = asString(m.get("title"));
            var summary = asString(m.get("summary"), m.get("content"), m.get("snippet"), m.get("description"));
            var tagsObj = m.get("tags");

            var hay = new StringBuilder();
            if (title  != null) hay.append(title).append(' ');
            if (summary!= null) hay.append(summary).append(' ');
            if (tagsObj instanceof List<?> l) {
                for (var t : l) hay.append(String.valueOf(t)).append(' ');
            } else if (tagsObj instanceof String s) {
                hay.append(s);
            }

            var text = hay.toString().toLowerCase();
            return kws.isEmpty() || kws.stream().anyMatch(text::contains);
        }).toList();

        var sorted = new ArrayList<>(filtered);
        sorted.sort(CrawlController::compareArticleLikeMaps);

        if (limit < 1) limit = 1;
        return ResponseEntity.ok(sorted.stream().limit(limit).toList());
    }

    // --------- 헬퍼들 ---------
    private static int compareArticleLikeMaps(Map<String,Object> a, Map<String,Object> b) {
        // 1) score desc
        Double sa = tryDouble(a.get("score"));
        Double sb = tryDouble(b.get("score"));
        if (!Objects.equals(sa, sb)) {
            return Double.compare(sb == null ? -1 : sb, sa == null ? -1 : sa);
        }
        // 2) publishedAt/date desc (문자열 비교로도 최신이 먼저 오도록)
        String da = asString(a.get("publishedAt"), a.get("date"), a.get("published"));
        String db = asString(b.get("publishedAt"), b.get("date"), b.get("published"));
        if (da != null && db != null && !da.equals(db)) {
            return db.compareTo(da);
        }
        // 3) title asc
        String ta = asString(a.get("title"));
        String tb = asString(b.get("title"));
        if (ta == null) ta = "";
        if (tb == null) tb = "";
        return ta.compareToIgnoreCase(tb);
    }

    private static String asString(Object... candidates) {
        for (Object c : candidates) {
            if (c instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    private static Double tryDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception ignored) {}
        }
        return null;
    }
}
