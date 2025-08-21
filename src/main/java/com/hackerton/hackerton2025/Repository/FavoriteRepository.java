// src/main/java/com/hackerton/hackerton2025/Repository/FavoriteRepository.java
package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // ===== 기본 조회 =====
    boolean existsByUserIdAndPost_Id(Long userId, Long postId);
    Optional<Favorite> findByUserIdAndPost_Id(Long userId, Long postId);

    // 파생 삭제 (사용 안 해도 됨)
    void deleteByUserIdAndPost_Id(Long userId, Long postId);

    @EntityGraph(attributePaths = "post")
    Page<Favorite> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "post")
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByPost_Id(Long postId);

    List<Favorite> findByUserIdAndPost_IdIn(Long userId, List<Long> postIds);

    /** 내 찜 postId만 최신순으로 (엔티티 미로딩) */
    @Query("select f.post.id from Favorite f where f.userId = :userId order by f.createdAt desc")
    List<Long> findPostIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /** 주어진 postIds 중 내가 찜한 postId만 */
    @Query("select f.post.id from Favorite f where f.userId = :userId and f.post.id in :postIds")
    List<Long> findPostIdsFavoritedByUserIn(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);

    long countByUserId(Long userId);

    // ===== 멱등/동시성 안전한 쓰기용 (예외 없이 0/1 반환) =====

    /** 중복이면 무시(0), 새로 추가되면 1 반환 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT IGNORE INTO favorite (user_id, post_id, created_at) " +
            "VALUES (:userId, :postId, NOW())", nativeQuery = true)
    int insertIgnore(@Param("userId") Long userId, @Param("postId") Long postId);

    /** 존재하면 1, 없으면 0 반환 (삭제) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM favorite WHERE user_id = :userId AND post_id = :postId", nativeQuery = true)
    int deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
}
