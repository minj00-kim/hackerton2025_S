package com.hackerton.hackerton2025.Service;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackerton.hackerton2025.Dto.TopDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor



public class TopDataService {
    private final PythonRunner python;
    private final ObjectMapper om;

    // 기본 설정 (application.properties에서 주입)
    @Value("${app.topdata.script:TopData.py}")
    private String defaultScript;

    @Value("${app.datasets.bc_csv:}")
    private String defaultCsv;

    @Value("${app.topdata.start:2019-01-01}")
    private String defaultStart;

    @Value("${app.topdata.end:2023-10-31}")
    private String defaultEnd;

    /**
     * GET /api/topdata?start=YYYY-MM-DD&end=YYYY-MM-DD
     * (옵션) &script=TopData.py&csv=C:\...\file.csv&workDir=C:\...\node-ai&bin=C:\...\python.exe
     */
    public TopDataDto run(
            String start,
            String end,
            String ovScript,   // optional override for script name
            String ovCsv,      // optional override for CSV absolute path
            String ovWorkDir,  // optional override for python working dir
            String ovBin       // optional override for python executable
    ) throws Exception {

        // ---- 파라미터/기본값 정리 ----
        String script = (ovScript != null && !ovScript.isBlank()) ? ovScript : defaultScript;
        String csv = (ovCsv != null && !ovCsv.isBlank()) ? ovCsv : defaultCsv;
        String s = (start == null || start.isBlank()) ? defaultStart : start;
        String e = (end == null || end.isBlank()) ? defaultEnd : end;

        // ---- 필수 파일 유효성 검사 ----
        if (csv == null || csv.isBlank() || !Files.exists(Path.of(csv))) {
            throw new IllegalStateException("CSV 경로가 올바르지 않습니다: " + csv);
        }

        // ---- 런타임 오버라이드(선택) ----
        // * 운영에서는 보안상 화이트리스트/비활성화 권장
        if (ovWorkDir != null && !ovWorkDir.isBlank()) {
            System.setProperty("app.python.workDir", ovWorkDir);
        }
        if (ovBin != null && !ovBin.isBlank()) {
            System.setProperty("app.python.exec", ovBin);
        }

        // ---- 파이썬 스크립트 인자 구성 ----
        List<String> args = new ArrayList<>();
        args.add("--csv");
        args.add(csv);
        args.add("--start");
        args.add(s);
        args.add("--end");
        args.add(e);
        args.add("--json-only");                // stdout으로 순수 JSON 한 줄 출력

        var res = python.run(script, args, Map.of(), Duration.ofMinutes(3));

        if (res.exitCode() != 0) {
            throw new IllegalStateException("TopData 실행 실패\n[stderr]\n" + res.stderr());
        }
        String out = res.stdout();
        if (out == null || out.isBlank()) {
            throw new IllegalStateException("TopData 실행은 성공했지만 출력이 비었습니다.");
        }

        // ---- JSON → DTO 변환 ----
        return om.readValue(out, TopDataDto.class);
    }
}
