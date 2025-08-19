package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.RegionBubble;
import com.hackerton.hackerton2025.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionAggregationService {
    private static final String CHUNGNAM_SIDO = "44"; // 충남

    private final PostRepository postRepository;

    /** [호환용] 시도 내 시군구 집계 (기존 동작: 매물 있는 곳만) */
    @Cacheable(cacheNames = "regions:sgg:summary", key = "#sidoCode")
    public List<RegionBubble> sggSummaryInSido(String sidoCode) {
        return postRepository.countBySggInSido(sidoCode).stream()
                .map(a -> new RegionBubble(
                        a.getCode(), a.getName(), a.getCnt(),
                        a.getAvgLat() == null ? 0 : a.getAvgLat(),
                        a.getAvgLng() == null ? 0 : a.getAvgLng()))
                .toList();
    }

    /** 시도 내 시군구 집계 includeZero 지원 */
    @Cacheable(cacheNames = "regions:sgg:summary", key = "#sidoCode + ':includeZero=' + #includeZero")
    public List<RegionBubble> sggSummaryInSido(String sidoCode, boolean includeZero) {
        var rows = includeZero
                ? postRepository.countBySggInSidoIncludeZero(sidoCode)
                : postRepository.countBySggInSido(sidoCode);

        return rows.stream()
                .map(a -> new RegionBubble(
                        a.getCode(), a.getName(), a.getCnt(),
                        a.getAvgLat() == null ? 0 : a.getAvgLat(),   // includeZero=true일 때는 region_sgg.center_lat
                        a.getAvgLng() == null ? 0 : a.getAvgLng()))  // includeZero=true일 때는 region_sgg.center_lng
                .toList();
    }

    /** 충남 전용 헬퍼 */
    @Cacheable(cacheNames = "regions:sgg:summary", key = "'44' + ':includeZero=' + #includeZero")
    public List<RegionBubble> chungnamSggSummary(boolean includeZero) {
        return sggSummaryInSido(CHUNGNAM_SIDO, includeZero);
    }

    /** [호환용] 시군구 내 읍면동 집계 (기존 동작: 매물 있는 곳만) */
    @Cacheable(cacheNames = "regions:sgg:summary", key = "'dong:' + #sggCode")
    public List<RegionBubble> dongSummaryInSgg(String sggCode) {
        return postRepository.countByDongInSgg(sggCode).stream()
                .map(a -> new RegionBubble(
                        a.getCode(), a.getName(), a.getCnt(),
                        a.getAvgLat() == null ? 0 : a.getAvgLat(),
                        a.getAvgLng() == null ? 0 : a.getAvgLng()))
                .toList();
    }

    /** 시군구 내 읍면동 집계 includeZero 지원 */
    @Cacheable(cacheNames = "regions:sgg:summary", key = "'dong:' + #sggCode + ':includeZero=' + #includeZero")
    public List<RegionBubble> dongSummaryInSgg(String sggCode, boolean includeZero) {
        var rows = includeZero
                ? postRepository.countByDongInSggIncludeZero(sggCode)
                : postRepository.countByDongInSgg(sggCode);

        return rows.stream()
                .map(a -> new RegionBubble(
                        a.getCode(), a.getName(), a.getCnt(),
                        a.getAvgLat() == null ? 0 : a.getAvgLat(),
                        a.getAvgLng() == null ? 0 : a.getAvgLng()))
                .toList();
    }
}
