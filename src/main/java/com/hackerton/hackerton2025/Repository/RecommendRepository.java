package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Recommend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendRepository extends JpaRepository<Recommend, Long> {

    List<Recommend> findByPostId(Long postId);

    void deleteByPostId(Long postId);
}