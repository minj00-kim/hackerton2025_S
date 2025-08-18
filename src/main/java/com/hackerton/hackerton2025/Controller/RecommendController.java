package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.AutoRecommendRequest;
import com.hackerton.hackerton2025.Dto.RecommendResponse;
import com.hackerton.hackerton2025.Service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/recommends")
public class RecommendController {


    private final RecommendService recommendService;

    /** 특정 게시물의 기존 추천 목록 조회 */
    @GetMapping
    public ResponseEntity<List<RecommendResponse>> getRecommends(@PathVariable Long postId) {
        return ResponseEntity.ok(recommendService.getRecommends(postId));
    }

    /** 자동 추천 2개 생성 (기존 추천 삭제 후 갱신) — 바디 또는 쿼리로 반경 입력 */
    @PostMapping("/auto")
    public ResponseEntity<List<RecommendResponse>> autoRecommend(
            @PathVariable Long postId,
            @RequestBody(required = false) AutoRecommendRequest body,
            @RequestParam(value = "radiusMeters", required = false) Double radiusParam
    ) {
        double radius = radiusParam != null
                ? radiusParam
                : (body != null && body.getRadiusMeters() != null ? body.getRadiusMeters() : 300.0);
        return ResponseEntity.ok(recommendService.autoRecommendTwo(postId, radius));
    }

}
