package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Security.GuestCookieFilter;
import com.hackerton.hackerton2025.Service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService svc;

    // ✅ 찜 추가 (postId 기준)
    @PostMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable Long postId, HttpServletRequest req){
        Long uid = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (uid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        svc.add(uid, postId);
    }

    // ✅ 찜 제거
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long postId, HttpServletRequest req){
        Long uid = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (uid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        svc.remove(uid, postId);
    }

    // ✅ 내 찜 목록 (postId 배열만 반환, 최신순)
    @GetMapping
    public List<Long> my(HttpServletRequest req){
        Long uid = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (uid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        return svc.myList(uid).stream().map(f -> f.getPost().getId()).toList();
    }

    // ✅ 특정 매물의 전체 찜 개수
    @GetMapping("/{postId}/count")
    public long count(@PathVariable Long postId){
        return svc.count(postId);
    }

    // ✅ 단건 체크: 내가 이 매물을 찜했는가? (쿠키 없으면 false)
    @GetMapping("/{postId}/me")
    public Map<String, Boolean> me(@PathVariable Long postId, HttpServletRequest req){
        Long uid = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        boolean favorited = svc.isMyFavorite(uid, postId);
        return Map.of("favorited", favorited);
    }

    // ✅ 배치 체크: 여러 매물의 찜 여부 한 번에 (예: /api/favorites/me?ids=2,3,4)
    @GetMapping("/me")
    public Map<Long, Boolean> meBatch(@RequestParam String ids, HttpServletRequest req){
        if (ids == null || ids.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids 파라미터가 필요합니다 (예: ids=1,2,3)");
        }
        List<Long> postIds;
        try {
            postIds = Arrays.stream(ids.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::parseLong).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids는 숫자 목록이어야 합니다");
        }

        Long uid = (Long) req.getAttribute(GuestCookieFilter.ATTR); // null이어도 서비스에서 false 처리
        return svc.areMyFavorites(uid, postIds);
    }
}
