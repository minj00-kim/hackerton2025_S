package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.RecommendResponse;
import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Entity.Recommend;
import com.hackerton.hackerton2025.Repository.PostRepository;
import com.hackerton.hackerton2025.Repository.RecommendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor


public class RecommendService {


    private final RecommendRepository recommendRepository;
    private final PostRepository postRepository;

    /** 프로젝트에서 쓰는 5개 카테고리 */
    private static final List<String> CANONICAL_CATEGORIES =
            List.of("카페", "편의점", "치킨", "약국", "주점");

    /** 간단 정규화 매핑(옵션) */
    private static final Map<String, String> CANON_MAP = Map.of(
            "cafe", "카페",
            "편의 점", "편의점",
            "치 킨", "치킨",
            "약 국", "약국",
            "bar", "주점"
    );

    @Transactional(readOnly = true)
    public List<RecommendResponse> getRecommends(Long postId) {
        return recommendRepository.findByPostId(postId).stream()
                .map(r -> new RecommendResponse(
                        r.getId(), r.getCategory(), r.getScore(), r.getReason(), r.getPost().getId()
                ))
                .toList(); // Java 16+; 하위 버전이면 .collect(Collectors.toList())
    }

    /**
     * 자동 추천:
     * - 반경 내 분포를 보고 가장 드문 카테고리 2개 선정
     * - score = 1 / (count + 1)
     * - 기존 추천은 전부 삭제 후 갱신
     */
    @Transactional
    public List<RecommendResponse> autoRecommendTwo(Long postId, double radiusMeters) {
        if (radiusMeters <= 0) {
            throw new IllegalArgumentException("radiusMeters는 0보다 커야 합니다.");
        }

        Post base = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post 없음: " + postId));

        if (!hasValidCoord(base)) {
            throw new IllegalStateException("기준 Post에 좌표 없음 또는 범위 초과 (latitude/longitude)");
        }

        // 주변 게시물(자기 자신 제외)
        var nearby = postRepository.findNearby(
                        base.getLatitude(), base.getLongitude(), radiusMeters
                ).stream()
                .filter(p -> !Objects.equals(p.getId(), postId))
                .toList();

        // 카테고리 빈도 집계
        Map<String, Long> counts = new HashMap<>();
        for (String cat : CANONICAL_CATEGORIES) counts.put(cat, 0L);

        for (Post p : nearby) {
            String cat = normalize(p.getCategory());
            if (cat != null && counts.containsKey(cat)) {
                counts.put(cat, counts.get(cat) + 1);
            }
        }

        // 드문 카테고리 상위 2개
        var top2 = counts.entrySet().stream()
                .sorted(Comparator
                        .comparingLong(Map.Entry<String, Long>::getValue)
                        .thenComparing(Map.Entry::getKey))
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();

        // 기존 추천 일괄 삭제
        recommendRepository.deleteByPostId(postId);

        List<RecommendResponse> result = new ArrayList<>();
        for (String cat : top2) {
            long cnt = counts.getOrDefault(cat, 0L);
            double score = 1.0 / (cnt + 1.0);
            String reason = String.format("반경 %.0fm 내 '%s' 업종이 %d건으로 비교적 드뭅니다.", radiusMeters, cat, cnt);

            Recommend saved = recommendRepository.save(
                    Recommend.builder()
                            .category(cat)
                            .score(score)
                            .reason(reason)
                            .post(base)
                            .build()
            );

            result.add(new RecommendResponse(
                    saved.getId(), saved.getCategory(), saved.getScore(), saved.getReason(), base.getId()
            ));
        }
        return result;
    }

    private boolean hasValidCoord(Post p) {
        if (p.getLatitude() == null || p.getLongitude() == null) return false;
        double lat = p.getLatitude();
        double lng = p.getLongitude();
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    private String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // 간단 매핑 우선 적용
        if (CANON_MAP.containsKey(s)) return CANON_MAP.get(s);
        // 소문자/공백 제거 후 매핑
        String compact = s.replaceAll("\\s+", "").toLowerCase();
        for (var e : CANON_MAP.entrySet()) {
            if (compact.equals(e.getKey().replaceAll("\\s+", "").toLowerCase())) {
                return e.getValue();
            }
        }
        return s; // 그대로 반환(카테고리 집합 밖이면 무시됨)
    }
}
