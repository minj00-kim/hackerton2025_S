// src/main/java/com/hackerton/hackerton2025/Controller/RegionController.java
package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.RegionBubble;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Service.PostService;
import com.hackerton.hackerton2025.Service.RegionAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionAggregationService agg;
    private final PostService postService;

    private int clamp(int v, int min, int max){ return Math.min(max, Math.max(min, v)); }

    // 1) (편의) 충남 시군구 말풍선
    //    /regions/sido/chungnam/summary?includeZero=true
    @GetMapping("/sido/chungnam/summary")
    public ResponseEntity<List<RegionBubble>> chungnamSggSummary(
            @RequestParam(defaultValue = "false") boolean includeZero
    ) {
        return ResponseEntity.ok(agg.sggSummaryInSido("44", includeZero)); // 충남
    }

    // 2) 일반 시도코드로 시군구 말풍선
    //    /regions/sido/{sidoCode}/summary?includeZero=true
    @GetMapping("/sido/{sidoCode}/summary")
    public ResponseEntity<List<RegionBubble>> sggSummary(
            @PathVariable String sidoCode,
            @RequestParam(defaultValue = "false") boolean includeZero
    ) {
        return ResponseEntity.ok(agg.sggSummaryInSido(sidoCode, includeZero));
    }

    // 3) 시군구 클릭 → 읍면동 말풍선
    //    /regions/sgg/{sggCode}/summary?includeZero=true
    @GetMapping("/sgg/{sggCode}/summary")
    public ResponseEntity<List<RegionBubble>> dongSummary(
            @PathVariable String sggCode,
            @RequestParam(defaultValue = "false") boolean includeZero
    ) {
        return ResponseEntity.ok(agg.dongSummaryInSgg(sggCode, includeZero));
    }

    // 4) 시군구 클릭 → 해당 매물 리스트(페이지)
    @GetMapping("/sgg/{sggCode}/posts")
    public ResponseEntity<Page<PostResponse>> listBySgg(@PathVariable String sggCode,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(postService.listBySgg(sggCode,  clamp(page,0,100000), clamp(size,1,100)));
    }

    // 5) 읍면동 클릭 → 해당 매물 리스트(페이지)
    @GetMapping("/dong/{dongCode}/posts")
    public ResponseEntity<Page<PostResponse>> listByDong(@PathVariable String dongCode,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(postService.listByDong(dongCode, clamp(page,0,100000), clamp(size,1,100)));
    }
}
