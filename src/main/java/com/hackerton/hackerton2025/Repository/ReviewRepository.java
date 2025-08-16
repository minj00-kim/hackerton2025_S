// src/main/java/com/hackerton/hackerton2025/Repository/ReviewRepository.java
package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 목록 (전체, 정렬: createdAt desc)
    List<Review> findByListing_IdOrderByCreatedAtDesc(Long listingId);

    // 페이지네이션 목록
    Page<Review> findByListing_Id(Long listingId, Pageable pageable);

    // 수정/삭제 시 본인 것만 찾기
    Optional<Review> findByIdAndListing_IdAndUserId(Long id, Long listingId, Long userId);

    // 중복 작성 방지용
    boolean existsByListing_IdAndUserId(Long listingId, Long userId);

    // 평균 평점 (없으면 0)
    @Query("select coalesce(avg(r.rating), 0) from Review r where r.listing.id = :listingId")
    Double avgRating(@Param("listingId") Long listingId);

    // 총 리뷰 개수
    long countByListing_Id(Long listingId);
}
