// src/main/java/com/hackerton/hackerton2025/Repository/PostRepository.java
package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // ===== 기본 조회 =====
    List<Post> findByOwnerId(Long ownerId);
    Page<Post> findByOwnerId(Long ownerId, Pageable pageable);

    List<Post> findByCategory(String category);
    Page<Post> findByCategory(String category, Pageable pageable);

    List<Post> findAllByOrderByCreatedAtDesc();
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Post> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng
    );
    Page<Post> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng, Pageable pageable
    );

    // 반경 검색(하버사인) — 리스트 버전
    @Query(value = """
        SELECT * FROM posts p
        WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL
          AND (
            2 * 6371000 * ASIN(SQRT(
                POWER(SIN(RADIANS((:lat - p.latitude) / 2)), 2) +
                COS(RADIANS(p.latitude)) * COS(RADIANS(:lat)) *
                POWER(SIN(RADIANS((:lng - p.longitude) / 2)), 2)
            ))
          ) <= :radius
        """, nativeQuery = true)
    List<Post> findNearby(@Param("lat") double lat,
                          @Param("lng") double lng,
                          @Param("radius") double radiusMeters);

    // 카테고리 집계
    @Query("""
           select p.category as category, count(p) as cnt
           from Post p
           where p.category is not null and p.category <> ''
           group by p.category
           """)
    List<CategoryCountView> countByCategory();

    interface CategoryCountView {
        String getCategory();
        long getCnt();
    }

    // ===== 지역 집계/조회 =====

    /** 네이티브 집계 결과 매핑용 프로젝션 */
    interface RegionAgg {
        String getCode();    // sgg_code 또는 dong_code
        String getName();    // sigungu 또는 dong
        long   getCnt();
        Double getAvgLat();
        Double getAvgLng();
    }

    /** 시도 내 시군구별 개수(충남: sidoCode='34') */
    @Query(value = """
            SELECT p.sgg_code            AS code,
                   COALESCE(p.sigungu, '') AS name,
                   COUNT(*)               AS cnt,
                   AVG(p.latitude)        AS avgLat,
                   AVG(p.longitude)       AS avgLng
            FROM posts p
            WHERE p.sido_code = :sido
            GROUP BY p.sgg_code, p.sigungu
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<RegionAgg> countBySggInSido(@Param("sido") String sidoCode);

    /** 시군구 내 읍면동별 개수 */
    @Query(value = """
            SELECT p.dong_code           AS code,
                   COALESCE(p.dong, '')  AS name,
                   COUNT(*)              AS cnt,
                   AVG(p.latitude)       AS avgLat,
                   AVG(p.longitude)      AS avgLng
            FROM posts p
            WHERE p.sgg_code = :sgg
            GROUP BY p.dong_code, p.dong
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<RegionAgg> countByDongInSgg(@Param("sgg") String sggCode);

    /** 지역별 목록(최신순) */
    Page<Post> findBySggCodeOrderByCreatedAtDesc(String sggCode, Pageable pageable);
    Page<Post> findByDongCodeOrderByCreatedAtDesc(String dongCode, Pageable pageable);

    /** 백필용: 좌표 O + 코드 누락 */
    @Query("""
       select p from Post p
       where p.latitude is not null and p.longitude is not null
         and (p.sidoCode is null or p.sggCode is null or p.dongCode is null)
       """)
    Page<Post> findMissingRegionCodesWithCoords(Pageable pageable);

    /** 백필용: 코드 누락(좌표 여부 무관) */
    @Query("""
       select p from Post p
       where (p.sidoCode is null or p.sggCode is null or p.dongCode is null)
       """)
    Page<Post> findMissingRegionCodes(Pageable pageable);

    /** 시도 내 시군구별 개수 — 매물 0건 지역 포함 */
    @Query(value = """
    SELECT s.sgg_code              AS code,
           s.name                  AS name,
           COALESCE(COUNT(p.id), 0) AS cnt,
           /* 마스터 중심좌표를 avgLat/avgLng 이름으로 내려서 기존 프로젝션 재사용 */
           s.center_lat            AS avgLat,
           s.center_lng            AS avgLng
    FROM region_sgg s
    LEFT JOIN posts p ON p.sgg_code = s.sgg_code
    WHERE s.sido_code = :sido
    GROUP BY s.sgg_code, s.name, s.center_lat, s.center_lng
    ORDER BY cnt DESC, s.name ASC
    """, nativeQuery = true)
    List<RegionAgg> countBySggInSidoIncludeZero(@Param("sido") String sidoCode);

    /** 시군구 내 읍면동별 개수 — 매물 0건 지역 포함 (region_dong 준비 후 사용) */
    @Query(value = """
    SELECT d.dong_code             AS code,
           d.name                  AS name,
           COALESCE(COUNT(p.id), 0) AS cnt,
           d.center_lat            AS avgLat,
           d.center_lng            AS avgLng
    FROM region_dong d
    LEFT JOIN posts p ON p.dong_code = d.dong_code
    WHERE d.sgg_code = :sgg
    GROUP BY d.dong_code, d.name, d.center_lat, d.center_lng
    ORDER BY cnt DESC, d.name ASC
    """, nativeQuery = true)
    List<RegionAgg> countByDongInSggIncludeZero(@Param("sgg") String sggCode);
}
