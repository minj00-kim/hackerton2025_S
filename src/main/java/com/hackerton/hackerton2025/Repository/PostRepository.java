// src/main/java/com/hackerton/hackerton2025/Repository/PostRepository.java
package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Entity.DealType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    interface RegionAgg {
        String getCode();
        String getName();
        long   getCnt();
        Double getAvgLat();
        Double getAvgLng();
    }

    @Query(value = """
            SELECT p.sgg_code              AS code,
                   COALESCE(p.sigungu, '') AS name,
                   COUNT(*)                AS cnt,
                   AVG(p.latitude)         AS avgLat,
                   AVG(p.longitude)        AS avgLng
            FROM posts p
            WHERE p.sido_code = :sido
            GROUP BY p.sgg_code, p.sigungu
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<RegionAgg> countBySggInSido(@Param("sido") String sidoCode);

    @Query(value = """
            SELECT p.dong_code             AS code,
                   COALESCE(p.dong, '')    AS name,
                   COUNT(*)                AS cnt,
                   AVG(p.latitude)         AS avgLat,
                   AVG(p.longitude)        AS avgLng
            FROM posts p
            WHERE p.sgg_code = :sgg
            GROUP BY p.dong_code, p.dong
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<RegionAgg> countByDongInSgg(@Param("sgg") String sggCode);

    Page<Post> findBySggCodeOrderByCreatedAtDesc(String sggCode, Pageable pageable);
    Page<Post> findByDongCodeOrderByCreatedAtDesc(String dongCode, Pageable pageable);

    @Query("""
       select p from Post p
       where p.latitude is not null and p.longitude is not null
         and (p.sidoCode is null or p.sggCode is null or p.dongCode is null)
       """)
    Page<Post> findMissingRegionCodesWithCoords(Pageable pageable);

    @Query("""
       select p from Post p
       where (p.sidoCode is null or p.sggCode is null or p.dongCode is null)
       """)
    Page<Post> findMissingRegionCodes(Pageable pageable);

    @Query(value = """
    SELECT s.sgg_code              AS code,
           s.name                  AS name,
           COALESCE(COUNT(p.id), 0) AS cnt,
           s.center_lat            AS avgLat,
           s.center_lng            AS avgLng
    FROM region_sgg s
    LEFT JOIN posts p ON p.sgg_code = s.sgg_code
    WHERE s.sido_code = :sido
    GROUP BY s.sgg_code, s.name, s.center_lat, s.center_lng
    ORDER BY cnt DESC, s.name ASC
    """, nativeQuery = true)
    List<RegionAgg> countBySggInSidoIncludeZero(@Param("sido") String sidoCode);

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

    // ===== 검색/정렬 =====
    Page<Post> findByDealType(DealType dealType, Pageable pageable);

    // ===== 인기 관련 카운터(원자적 업데이트) =====
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.favCount = p.favCount + 1 where p.id = :id")
    int incFav(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.favCount = case when p.favCount>0 then p.favCount-1 else 0 end where p.id = :id")
    int decFav(@Param("id") Long id);

    // ✅ 조회수도 원자적으로 증가시키는 메서드 추가 (중복 증가/경쟁 조건 방지)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.views = p.views + 1 where p.id = :id")
    int incViews(@Param("id") Long id);
}
