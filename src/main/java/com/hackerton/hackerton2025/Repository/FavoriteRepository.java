package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    boolean existsByUserIdAndListing_Id(Long userId, Long listingId);
    Optional<Favorite> findByUserIdAndListing_Id(Long userId, Long listingId);
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByListing_Id(Long listingId);

    // ✅ 추가: 여러 listingId를 한 번에 체크 (목록 화면 하트 상태용)
    List<Favorite> findByUserIdAndListing_IdIn(Long userId, List<Long> listingIds);
}