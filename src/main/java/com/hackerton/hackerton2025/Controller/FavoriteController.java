// src/main/java/com/hackerton/hackerton2025/Controller/FavoriteController.java
package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Security.GuestCookieFilter;
import com.hackerton.hackerton2025.Service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService svc;

    @PostMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable Long listingId, HttpServletRequest req){
        Long uid = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (uid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        svc.add(uid, listingId);
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long listingId, HttpServletRequest req){
        Long uid = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (uid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        svc.remove(uid, listingId);
    }

    // 내 찜 목록 (listingId 배열만 반환)
    @GetMapping
    public List<Long> my(HttpServletRequest req){
        Long uid = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (uid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        return svc.myList(uid).stream().map(f -> f.getListing().getId()).toList();
    }

    // 특정 매물의 전체 찜 개수
    @GetMapping("/{listingId}/count")
    public long count(@PathVariable Long listingId){
        return svc.count(listingId);
    }
}
