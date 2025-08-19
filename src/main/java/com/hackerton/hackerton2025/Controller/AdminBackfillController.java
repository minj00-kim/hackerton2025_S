// src/main/java/com/hackerton/hackerton2025/Controller/AdminBackfillController.java
package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Service.BackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/backfill")
@RequiredArgsConstructor
public class AdminBackfillController {

    private final BackfillService backfillService;

    // 예: /admin/backfill/regions?prefix=충남
    @PostMapping("/regions")
    public ResponseEntity<Map<String,Object>> backfillRegions(
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam(defaultValue = "200") int batchSize,
            @RequestParam(defaultValue = "50") int maxBatches,
            @RequestParam(defaultValue = "true") boolean geocodeIfMissing
    ) {
        boolean onlyChungnam = isChungnam(prefix);
        String summary = backfillService.backfillRegionCodes(batchSize, maxBatches, onlyChungnam, geocodeIfMissing);
        return ResponseEntity.ok(Map.of(
                "prefix", prefix,
                "onlyChungnam", onlyChungnam,
                "summary", summary
        ));
    }

    private boolean isChungnam(String prefix) {
        if (prefix == null || prefix.isBlank()) return false;
        String p = prefix.trim();
        // 코드/이름 아무거나 허용
        return "34".equals(p) || p.contains("충남") || p.contains("충청남도");
    }
}
