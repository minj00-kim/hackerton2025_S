// src/main/java/com/hackerton/hackerton2025/Config/CrawlStartupRunner.java
package com.hackerton.hackerton2025.Config;

import com.hackerton.hackerton2025.Service.PythonRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlStartupRunner implements ApplicationRunner {

    private final PythonRunner py;

    @Value("${app.crawl.startup.enabled:true}")
    boolean enabled;

    // 쉼표로만 구분 (공백 허용): 예) "부동산 정책,청년 전세자금,전월세, 창업"
    @Value("${app.crawl.startup.query:부동산 정책}")
    String queryRaw;

    @Value("${app.crawl.startup.max:12}")
    int maxItems;

    @Value("${app.exports.crawlDir}")
    String crawlBase;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            log.info("[CrawlStartup] disabled -> skip");
            return;
        }

        // "부동산 정책,청년 전세자금,전월세, 창업"
        //  -> ["부동산 정책","청년 전세자금","전월세","창업"]
        List<String> queries = Arrays.stream(queryRaw.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        if (queries.isEmpty()) {
            log.warn("[CrawlStartup] no query -> skip");
            return;
        }

        maxItems = Math.max(1, maxItems);
        log.info("[CrawlStartup] queries={}, maxItems={}, out={}", queries, maxItems, crawlBase);

        // 파이썬 환경변수
        Map<String,String> env = new HashMap<>();
        if (crawlBase != null && !crawlBase.isBlank()) {
            env.put("CRAWL_BASE_OUT", crawlBase);
        }
        env.put("CRAWL_FETCH_OG", "1");            // 본문 og/트위터/JSON-LD 이미지 시도
        env.put("CRAWL_FAVICON_FALLBACK", "1");   // 이미지 없으면 파비콘으로 대체
        env.put("CRAWL_TIMEOUT", "10");           // 각 요청 타임아웃(초)
        env.put("CRAWL_SLEEP_MS", "50");          // 기사 루프 간 살짝 쉬기(ms)

        // 윈도우 한글 인자/출력 깨짐 방지
        env.put("PYTHONUTF8", "1");
        env.put("PYTHONIOENCODING", "utf-8");

        // 파이썬 인자 구성: --query <q> 를 쿼리 개수만큼 반복
        List<String> pyArgs = new ArrayList<>();
        pyArgs.add("crawl_youth_grants.py");
        for (String q : queries) {
            pyArgs.add("--query");
            pyArgs.add(q);
        }
        pyArgs.add("--max-items");
        pyArgs.add(String.valueOf(maxItems));

        var res = py.run(pyArgs.get(0), pyArgs.subList(1, pyArgs.size()), env, Duration.ofMinutes(6));
        if (res.exitCode() != 0) {
            log.warn("[CrawlStartup] 실패\n{}", res.stderr());
        } else {
            log.info("[CrawlStartup] 완료: {}", res.stdout().strip());
        }
    }
}
