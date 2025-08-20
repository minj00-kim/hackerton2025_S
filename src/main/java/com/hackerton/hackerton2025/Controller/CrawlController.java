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
            @RequestParam(defaultValue = "40") String maxItems
    ) throws Exception {

        List<String> args = List.of("crawl_youth_grants.py", "--query", query, "--max-items", maxItems);

        Map<String,String> env = new HashMap<>();
        String apiKey = System.getenv("OPENAI_API_KEY"); // 요약 쓰는 경우에만 필요
        if (apiKey != null) env.put("OPENAI_API_KEY", apiKey);

        var res = py.run(args.get(0), args.subList(1,args.size()), env, Duration.ofMinutes(6));
        if (res.exitCode() != 0) {
            return ResponseEntity.internalServerError().body(Map.of("stderr", res.stderr()));
        }

        var latestDir = store.findLatestDir(Path.of(crawlBase));
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");
        return ResponseEntity.ok(Map.of("dir", latestDir.toString(), "json", jsonPath.toString(), "stdout", res.stdout()));
    }

    // 최신 결과 반환
    @GetMapping("/latest")
    public ResponseEntity<?> latest() throws Exception {
        var latestDir = store.findLatestDir(Path.of(crawlBase));
        var jsonPath  = latestDir.resolve("youth_grants_extracted.json");
        var data = store.readArrayJson(jsonPath);
        return ResponseEntity.ok(data);

    }


}
