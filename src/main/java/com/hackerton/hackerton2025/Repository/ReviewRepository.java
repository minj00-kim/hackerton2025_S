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

    // ✅ 목록 (전체, 최신순)
    List<Review> findByPost_IdOrderByCreatedAtDesc(Long postId);

    // ✅ 페이지네이션 목록
    Page<Review> findByPost_Id(Long postId, Pageable pageable);

    // ✅ 수정/삭제 시 본인 것만 찾기
    Optional<Review> findByIdAndPost_IdAndUserId(Long id, Long postId, Long userId);

    // ✅ 중복 작성 방지
    boolean existsByPost_IdAndUserId(Long postId, Long userId);

    // (선택) 내가 쓴 리뷰 1건 찾기
    Optional<Review> findByPost_IdAndUserId(Long postId, Long userId);

    // ✅ 평균 평점 (null 가능 → 서비스에서 0.0 처리/반올림)
    @Query("select avg(r.rating) from Review r where r.post.id = :postId")
    Double avgRating(@Param("postId") Long postId);

    // ✅ 총 리뷰 개수
    long countByPost_Id(Long postId);

    long countByPostId(Long postId);
    List<Review> findTop10ByPostIdOrderByCreatedAtDesc(Long postId);
}
