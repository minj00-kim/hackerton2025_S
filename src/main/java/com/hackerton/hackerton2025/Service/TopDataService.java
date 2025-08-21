package com.hackerton.hackerton2025.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackerton.hackerton2025.Dto.TopDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor


public class TopDataService {


    private final PythonRunner python;
    private final ObjectMapper om;

    @Value("${app.python.exec:python}")
    private String pyExec;

    @Value("${app.python.workDir:.}")
    private String workDir;

    @Value("${app.datasets.bc_csv:}")
    private String csvPath;

    @Value("${app.topdata.start:2019-01-01}")
    private String defaultStart;

    @Value("${app.topdata.end:2023-10-31}")
    private String defaultEnd;

    public TopDataDto run(String start, String end) throws Exception {
        String s = (start == null || start.isBlank()) ? defaultStart : start;
        String e = (end == null || end.isBlank()) ? defaultEnd   : end;

        // TopData.py가 argparse를 쓰지 않아도, 우리가 환경변수로 전달하거나
        // sys.argv를 읽게 수정할 수 있음. 여기서는 환경변수 넘기는 예시 X,
        // 대신 TopData.py가 내부에서 app.datasets.bc_csv를 기본 경로로 사용한다고 가정.
        List<String> args = List.of("TopData.py"); // 인자 필요하면 뒤에 "--start", s, "--end", e 추가

        Map<String, String> env = new HashMap<>();
        // 필요시 CSV 경로/기간을 환경변수로 넘기고, 파이썬에서 os.getenv로 읽게 할 수도 있음:
        env.put("TOPDATA_CSV", csvPath);
        env.put("TOPDATA_START", s);
        env.put("TOPDATA_END", e);

        var res = python.run(args.get(0), args.subList(1, args.size()), env, Duration.ofMinutes(2));
        if (res.exitCode() != 0) {
            throw new IllegalStateException("TopData.py 실패: " + res.stderr());
        }

        // stdout(단일 JSON 문자열)을 DTO로 역직렬화
        return om.readValue(res.stdout(), TopDataDto.class);
    }



}
