package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByListing_IdOrderByCreatedAtDesc(Long listingId);

    // 수정/삭제 시 본인 것만 찾기
    Optional<Review> findByIdAndListing_IdAndUserId(Long id, Long listingId, Long userId);

    // 중복 작성 방지용
    boolean existsByListing_IdAndUserId(Long listingId, Long userId);
}