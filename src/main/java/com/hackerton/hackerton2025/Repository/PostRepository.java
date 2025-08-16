package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 특정 유저가 등록한 매물 찾기
    List<Post> findByOwner(User owner);

    // 특정 업종 카테고리로 검색
    List<Post> findByCategory(String category);

    // 반경 내 매물 검색 (JPQL은 지원 X, 네이티브 쿼리 사용)
    List<Post> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng
    );
}
