package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
// 필요하면 @Query, @Param 추가 임포트

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // ✅ 소유자(쿠키 anon_id) 기준
    List<Post> findByOwnerId(Long ownerId);
    Page<Post> findByOwnerId(Long ownerId, Pageable pageable);

    // ✅ 카테고리
    List<Post> findByCategory(String category);
    Page<Post> findByCategory(String category, Pageable pageable);

    // ✅ 최신순 조회 (선택)
    List<Post> findAllByOrderByCreatedAtDesc();
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ✅ 위경도 범위(바운딩 박스) 검색
    List<Post> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng
    );
    Page<Post> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng, Pageable pageable
    );

    // 필요하면 여기서 반경 검색(Haversine) 네이티브 쿼리로 추가 가능
    // @Query(nativeQuery = true, value = " ... ", countQuery = " ... ")
    // Page<Post> findWithinRadius(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusKm") double radiusKm, Pageable pageable);
}
