package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.ReviewRequest;
import com.hackerton.hackerton2025.Dto.ReviewResponse;
import com.hackerton.hackerton2025.Security.GuestCookieFilter;
import com.hackerton.hackerton2025.Service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/listings/{listingId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // 작성
    @PostMapping
    public Long write(@PathVariable Long listingId,
                      @RequestBody @Valid ReviewRequest req,
                      HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        if (anonId == null) throw new IllegalStateException("익명 사용자 ID가 설정되지 않았습니다.");
        return reviewService.write(listingId, anonId, req);
    }

    // 수정
    @PutMapping("/{reviewId}")
    public Long update(@PathVariable Long listingId,
                       @PathVariable Long reviewId,
                       @RequestBody @Valid ReviewRequest req,
                       HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        if (anonId == null) throw new IllegalStateException("익명 사용자 ID가 설정되지 않았습니다.");
        return reviewService.update(listingId, reviewId, anonId, req);
    }

    // 삭제
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long listingId,
                       @PathVariable Long reviewId,
                       HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        if (anonId == null) throw new IllegalStateException("익명 사용자 ID가 설정되지 않았습니다.");
        reviewService.delete(listingId, reviewId, anonId);
    }

    // 목록
    @GetMapping
    public List<ReviewResponse> list(@PathVariable Long listingId) {
        return reviewService.list(listingId);
    }
}