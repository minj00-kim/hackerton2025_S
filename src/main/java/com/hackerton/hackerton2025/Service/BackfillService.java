package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackfillService {

    private final PostRepository postRepository;
    private final KakaoGeoService kakaoGeoService;          // 주소 → 좌표
    private final KakaoRegionService kakaoRegionService;    // 좌표 → 시/군/동/코드

    /**
     * 지역 코드 백필 (좌표 있는 행 우선)
     * @param batchSize     페이지 당 처리 개수 (예: 200)
     * @param maxBatches    최대 페이지 수 (예: 50) - 안전장치
     * @param onlyChungnam  충남만 남길지 여부 (sido가 "충청남도" 또는 sidoCode == "34" 가정)
     * @param geocodeIfMissing 좌표 없을 때 주소로 지오코딩 시도할지
     * @return 처리 요약 문자열
     */
    @Transactional
    public String backfillRegionCodes(int batchSize, int maxBatches, boolean onlyChungnam, boolean geocodeIfMissing) {
        int pageIdx = 0;
        long updated = 0, skipped = 0, failed = 0;

        while (pageIdx < maxBatches) {
            Page<Post> page = postRepository.findMissingRegionCodes(PageRequest.of(pageIdx, batchSize));
            if (page.isEmpty()) break;

            for (Post p : page.getContent()) {
                try {
                    // 1) 좌표 확보
                    Double lat = p.getLatitude();
                    Double lng = p.getLongitude();

                    if ((lat == null || lng == null) && geocodeIfMissing) {
                        var geo = kakaoGeoService.geocode(p.getAddress()).orElse(null);
                        if (geo != null) {
                            lat = geo.lat();
                            lng = geo.lng();
                            p.setLatitude(lat);
                            p.setLongitude(lng);
                        }
                    }
                    if (lat == null || lng == null) { skipped++; continue; }

                    // 2) 좌표 → 행정구역
                    var regOpt = kakaoRegionService.coord2region(lat, lng);
                    if (regOpt.isEmpty()) { skipped++; continue; }
                    var reg = regOpt.get();

                    // 3) 충남만 필터 (원하면)
                    if (onlyChungnam) {
                        // 이름 기준
                        boolean okByName = "충청남도".equals(reg.sido());
                        // 코드 기준(카카오 코드 체계가 다르면 이 라인은 주석)
                        boolean okByCode = "34".equals(reg.sidoCode());
                        if (!(okByName || okByCode)) { skipped++; continue; }
                    }

                    // 4) 세팅
                    p.setSido(reg.sido());
                    p.setSigungu(reg.sigungu());
                    p.setDong(reg.dong());
                    p.setSidoCode(reg.sidoCode());
                    p.setSggCode(reg.sggCode());
                    p.setDongCode(reg.dongCode());

                    updated++;
                } catch (Exception e) {
                    failed++;
                    log.warn("[BACKFILL] postId={} 실패: {}", p.getId(), e.getMessage());
                }
            }

            // JPA 더티체킹으로 일괄 flush
            pageIdx++;
        }

        String summary = String.format("backfill done. updated=%d, skipped=%d, failed=%d, pages=%d",
                updated, skipped, failed, pageIdx);
        log.info(summary);
        return summary;
    }
}
