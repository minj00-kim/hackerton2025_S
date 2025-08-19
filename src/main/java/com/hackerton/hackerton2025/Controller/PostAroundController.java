package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Service.AroundService;
import com.hackerton.hackerton2025.Service.KakaoPlacesService;
import com.hackerton.hackerton2025.Service.PostService;
import com.hackerton.hackerton2025.Dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts/{postId}/around")
@RequiredArgsConstructor
public class PostAroundController {

    private final AroundService around;
    private final PostService postService;

    private int clamp(int v, int min, int max) { return Math.min(max, Math.max(min, v)); }

    // 상세 페이지 첫 진입용: 카테고리별 개수 요약
    @GetMapping("/summary")
    public ResponseEntity<Map<String,Object>> summaryForPost(@PathVariable Long postId,
                                                             @RequestParam(defaultValue = "800") int radius) {
        PostResponse p = postService.getPost(postId); // 404는 내부에서 처리됨
        if (p.getLatitude() == null || p.getLongitude() == null) {
            // 좌표가 비어있는 레거시 데이터 방어
            return ResponseEntity.badRequest().body(Map.of(
                    "postId", postId,
                    "error", "해당 매물에 좌표 정보가 없습니다."
            ));
        }
        int r = clamp(radius, 0, 20000);
        var buckets = around.summary(p.getLatitude(), p.getLongitude(), r);
        return ResponseEntity.ok(Map.of(
                "postId", postId,
                "lat", p.getLatitude(),
                "lng", p.getLongitude(),
                "radius", r,
                "buckets", buckets
        ));
    }

    // 카테고리 클릭 시: 주변 업소 목록
    @GetMapping("/places")
    public ResponseEntity<List<KakaoPlacesService.Poi>> placesForPost(@PathVariable Long postId,
                                                                      @RequestParam String category,
                                                                      @RequestParam(defaultValue = "800") int radius,
                                                                      @RequestParam(defaultValue = "20") int size) {
        PostResponse p = postService.getPost(postId);
        if (p.getLatitude() == null || p.getLongitude() == null) {
            return ResponseEntity.badRequest().build();
        }
        int r = clamp(radius, 0, 20000);
        int s = clamp(size, 1, 45);
        return ResponseEntity.ok(around.places(p.getLatitude(), p.getLongitude(), r, category, s));
    }


}
