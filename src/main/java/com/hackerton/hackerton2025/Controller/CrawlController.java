// src/main/java/com/hackerton/hackerton2025/Controller/CrawlController.java
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
            @RequestParam(required = false) List<String> query,   // 여러 개 쿼리 허용
            @RequestParam(defaultValue = "40") int maxItems
    ) throws Exception {

        // 빈/누락이면 기본값 하나 넣기
        if (query == null || query.isEmpty()) {
            query = List.of("청년창업 지원금");
        } else if (query.size() == 1 && query.get(0) != null && query.get(0).contains(",")) {
            // "부동산 정책,청년 전세자금,전월세, 창업" 처럼 한 파라미터에 쉼표로 들어온 경우 분해
            query = Arrays.stream(query.get(0).split("\\s*,\\s*"))
                    .map(String::trim).filter(s -> !s.isBlank()).toList();
        }

        maxItems = Math.max(1, maxItems);

        // 파이썬 인자 구성
        List<String> args = new ArrayList<>();
        args.add("crawl_youth_grants.py");
        for (String q : query) {
            if (q != null && !q.isBlank()) {
                args.add("--query");
                args.add(q);
            }
        }
        args.add("--max-items");
        args.add(String.valueOf(maxItems));

        Map<String,String> env = new HashMap<>();
        // 파이썬에 출력 경로/옵션 전달
        if (crawlBase != null && !crawlBase.isBlank()) {
            env.put("CRAWL_BASE_OUT", crawlBase);
        }
        env.put("CRAWL_FETCH_OG", "1");
        env.put("CRAWL_FAVICON_FALLBACK", "1");
        env.put("CRAWL_TIMEOUT", "10");
        env.put("CRAWL_SLEEP_MS", "50");
        env.put("PYTHONUTF8", "1");
        env.put("PYTHONIOENCODING", "utf-8");

        var res = py.run(args.get(0), args.subList(1, args.size()), env, Duration.ofMinutes(6));
        if (res.exitCode() != 0) {
            return ResponseEntity.internalServerError().body(Map.of("stderr", res.stderr()));
        }

        var latestDir = store.findLatestDir(Path.of(crawlBase));
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");

        // 결과 파일 존재 체크
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

    @GetMapping("/latest")
    public ResponseEntity<?> latest() throws Exception {
        var latestDir = store.findLatestDir(Path.of(crawlBase));
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");
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

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(defaultValue = "20") int limit) throws Exception {
        var latestDir = store.findLatestDir(Path.of(crawlBase));
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");
        if (!jsonPath.toFile().exists()) {
            return ResponseEntity.status(404).body(Map.of("error","RESULT_NOT_FOUND","expected", jsonPath.toString()));
        }
        var arr = store.readArrayJson(jsonPath); // List<Map<String,Object>>
        arr.sort(CrawlController::compareArticleLikeMaps);
        return ResponseEntity.ok(arr.stream().limit(Math.max(1, limit)).toList());
    }

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

        return ResponseEntity.ok(sorted.stream().limit(Math.max(1, limit)).toList());
    }

    private static int compareArticleLikeMaps(Map<String,Object> a, Map<String,Object> b) {
        Double sa = tryDouble(a.get("score"));
        Double sb = tryDouble(b.get("score"));
        if (!Objects.equals(sa, sb)) {
            return Double.compare(sb == null ? -1 : sb, sa == null ? -1 : sa);
        }
        String da = asString(a.get("publishedAt"), a.get("date"), a.get("published"));
        String db = asString(b.get("publishedAt"), b.get("date"), b.get("published"));
        if (da != null && db != null && !da.equals(db)) {
            return db.compareTo(da);
        }
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
