// src/main/java/com/hackerton/hackerton2025/Controller/AnalysisController.java
package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.MarketAnalysisRequest;
import com.hackerton.hackerton2025.Dto.MarketAnalysisResponse;
import com.hackerton.hackerton2025.Service.MarketAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/analysis", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AnalysisController {

    private final MarketAnalysisService service;

    /** 폼 제출 → 지표계산(카카오+SBIZ) → LLM요약 → JSON 응답 */
    @PostMapping(value = "/market", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MarketAnalysisResponse analyze(@RequestBody @Valid MarketAnalysisRequest req) {
        return service.analyze(req);
    }
}
