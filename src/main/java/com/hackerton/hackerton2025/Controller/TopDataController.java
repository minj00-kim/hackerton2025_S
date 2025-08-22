package com.hackerton.hackerton2025.Controller;


import com.hackerton.hackerton2025.Dto.TopDataDto;
import com.hackerton.hackerton2025.Service.TopDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topdata")
@RequiredArgsConstructor




public class TopDataController {

    private final TopDataService svc;

    // GET /api/topdata?start=2019-01-01&end=2023-10-31
    // (옵션) &script=TopData.py&csv=C:\path\file.csv&workDir=C:\...&bin=C:\...\python.exe
    @GetMapping
    public TopDataDto getTopData(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            // ----- 통제 파라미터(선택) -----
            @RequestParam(required = false) String script,
            @RequestParam(required = false) String csv,
            @RequestParam(required = false) String workDir,
            @RequestParam(required = false) String bin
    ) throws Exception {
        return svc.run(start, end, script, csv, workDir, bin);
    }
}
