// src/main/java/com/hackerton/hackerton2025/Service/FavoriteService.java
package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Entity.Favorite;
import com.hackerton.hackerton2025.Repository.FavoriteRepository;
import com.hackerton.hackerton2025.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favRepo;
    private final PostRepository postRepo;

    /** 찜 추가 (멱등 + 예외 없음) */
    public void add(Long userId, Long postId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다");
        // FK 위반 방지: 존재 확인 (경쟁 상황에서는 INSERT 시 FK로 안전)
        if (!postRepo.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매물을 찾을 수 없습니다");
        }

        // 새로 추가되면 1, 이미 있으면 0 (예외 없음)
        int affected = favRepo.insertIgnore(userId, postId);
        if (affected == 1) {
            postRepo.incFav(postId); // 처음 추가될 때만 +1
        }
    }

    /** 찜 제거 (멱등) */
    public void remove(Long userId, Long postId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다");

        // 존재하면 1, 없으면 0 (예외 없음)
        int removed = favRepo.deleteByUserIdAndPostId(userId, postId);
        if (removed == 1) {
            postRepo.decFav(postId); // 실제로 지워졌을 때만 -1 (음수 방지 쿼리 권장)
        }
        // 멱등: 안 지워졌어도 예외 던지지 않음 (컨트롤러는 204)
    }

    /** 내 찜 목록 (최신순) */
    @Transactional(readOnly = true)
    public List<Favorite> myList(Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다");
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
        if (userId == null) return false;
        return favRepo.existsByUserIdAndPost_Id(userId, postId);
    }

    /** 배치 체크: 여러 매물의 찜 여부 한 번에 */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> areMyFavorites(Long userId, List<Long> postIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (postIds != null) postIds.forEach(id -> result.put(id, false));
        if (userId == null || postIds == null || postIds.isEmpty()) return result;

        favRepo.findByUserIdAndPost_IdIn(userId, postIds)
                .forEach(f -> result.put(f.getPost().getId(), true));
        return result;
    }
}
