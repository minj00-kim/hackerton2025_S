package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Entity.Favorite;
import com.hackerton.hackerton2025.Entity.Listing;
import com.hackerton.hackerton2025.Repository.FavoriteRepository;
import com.hackerton.hackerton2025.Repository.ListingRepository;
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
    private final ListingRepository listingRepo;

    // 찜 추가 (멱등)
    public void add(Long userId, Long listingId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다");
        }

        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "매물을 찾을 수 없습니다"));

        if (favRepo.existsByUserIdAndListing_Id(userId, listingId)) {
            return; // 이미 찜됨 → 멱등
        }

        try {
            favRepo.save(Favorite.builder()
                    .userId(userId)
                    .listing(listing)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // 유니크 제약 동시요청 충돌 시에도 멱등하게 무시
        }
    }

    // 찜 제거
    public void remove(Long userId, Long listingId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다");
        }
        Favorite f = favRepo.findByUserIdAndListing_Id(userId, listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "찜된 상태가 아닙니다"));
        favRepo.delete(f);
    }

    // 내 찜 목록
    @Transactional(readOnly = true)
    public List<Favorite> myList(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다");
        }
        return favRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 특정 매물의 찜 개수
    @Transactional(readOnly = true)
    public long count(Long listingId) {
        return favRepo.countByListing_Id(listingId);
    }

    // ✅ 단건 체크: 내가 이 매물을 찜했는가?
    @Transactional(readOnly = true)
    public boolean isMyFavorite(Long userId, Long listingId) {
        if (userId == null) return false; // 쿠키 없으면 false
        return favRepo.existsByUserIdAndListing_Id(userId, listingId);
    }

    // ✅ 배치 체크: 여러 매물의 찜 여부 한 번에
    @Transactional(readOnly = true)
    public Map<Long, Boolean> areMyFavorites(Long userId, List<Long> listingIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (listingIds != null) {
            for (Long id : listingIds) result.put(id, false); // 기본 false
        }
        if (userId == null || listingIds == null || listingIds.isEmpty()) return result;

        favRepo.findByUserIdAndListing_IdIn(userId, listingIds)
                .forEach(f -> result.put(f.getListing().getId(), true));
        return result;
    }
}
