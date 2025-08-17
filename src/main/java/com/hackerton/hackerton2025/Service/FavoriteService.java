// src/main/java/com/hackerton/hackerton2025/Service/FavoriteService.java
package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Entity.Favorite;
import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Repository.FavoriteRepository;
import com.hackerton.hackerton2025.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favRepo;
    private final PostRepository postRepo;

    /** 찜 추가 (멱등) */
    public void add(Long userId, Long postId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다");
        }

        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "매물을 찾을 수 없습니다"));

        if (favRepo.existsByUserIdAndPost_Id(userId, postId)) {
            return; // 이미 찜됨 → 멱등
        }

        try {
            favRepo.save(Favorite.builder()
                    .userId(userId)
                    .post(post)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // 유니크 제약 동시요청 충돌 시에도 멱등하게 무시
        }
    }

    /** 찜 제거 */
    public void remove(Long userId, Long postId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다");
        }
        Favorite f = favRepo.findByUserIdAndPost_Id(userId, postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "찜된 상태가 아닙니다"));
        favRepo.delete(f);
    }

    /** 내 찜 목록 (최신순) */
    @Transactional(readOnly = true)
    public List<Favorite> myList(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다");
        }
        return favRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** 특정 매물의 찜 개수 */
    @Transactional(readOnly = true)
    public long count(Long postId) {
        return favRepo.countByPost_Id(postId);
    }

    /** 단건 체크: 내가 이 매물을 찜했는가? */
    @Transactional(readOnly = true)
    public boolean isMyFavorite(Long userId, Long postId) {
        if (userId == null) return false; // 쿠키 없으면 false
        return favRepo.existsByUserIdAndPost_Id(userId, postId);
    }

    /** 배치 체크: 여러 매물의 찜 여부 한 번에 */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> areMyFavorites(Long userId, List<Long> postIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (postIds != null) {
            for (Long id : postIds) result.put(id, false); // 기본 false
        }
        if (userId == null || postIds == null || postIds.isEmpty()) return result;

        favRepo.findByUserIdAndPost_IdIn(userId, postIds)
                .forEach(f -> result.put(f.getPost().getId(), true));
        return result;
    }
}
