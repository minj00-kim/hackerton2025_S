package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 단건 존재/조회 (userId + postId)
    boolean existsByUserIdAndPost_Id(Long userId, Long postId);
    Optional<Favorite> findByUserIdAndPost_Id(Long userId, Long postId);

    // 내 찜 목록(최신순)
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 게시글의 찜 개수
    long countByPost_Id(Long postId);

    // 여러 postId에 대한 내 찜 여부 일괄 조회
    List<Favorite> findByUserIdAndPost_IdIn(Long userId, List<Long> postIds);
}
