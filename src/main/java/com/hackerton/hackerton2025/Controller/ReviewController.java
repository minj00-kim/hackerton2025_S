// src/main/java/com/hackerton/hackerton2025/Controller/ReviewController.java
package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.RatingSummaryResponse;
import com.hackerton.hackerton2025.Dto.ReviewItemResponse;
import com.hackerton.hackerton2025.Dto.ReviewRequest;
import com.hackerton.hackerton2025.Dto.ReviewResponse;
import com.hackerton.hackerton2025.Security.GuestCookieFilter;
import com.hackerton.hackerton2025.Service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/listings/{listingId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성 (익명 쿠키 기반)
    @PostMapping
    public Long write(@PathVariable Long listingId,
                      @RequestBody @Valid ReviewRequest req,
                      HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        if (anonId == null) {
            throw new IllegalStateException("익명 사용자 ID가 설정되지 않았습니다.");
        }
        return reviewService.write(listingId, anonId, req);
    }

    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public Long update(@PathVariable Long listingId,
                       @PathVariable Long reviewId,
                       @RequestBody @Valid ReviewRequest req,
                       HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        if (anonId == null) {
            throw new IllegalStateException("익명 사용자 ID가 설정되지 않았습니다.");
        }
        return reviewService.update(listingId, reviewId, anonId, req);
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long listingId,
                       @PathVariable Long reviewId,
                       HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        if (anonId == null) {
            throw new IllegalStateException("익명 사용자 ID가 설정되지 않았습니다.");
        }
        reviewService.delete(listingId, reviewId, anonId);
    }

    // 리뷰 목록 (전체)
    @GetMapping
    public List<ReviewResponse> list(@PathVariable Long listingId) {
        return reviewService.list(listingId);
    }

    // ✅ 리뷰 목록 (페이지네이션 + 내가 쓴 리뷰 여부 포함)
    // 예: /api/listings/2/reviews/paged?page=0&size=10&sort=createdAt,desc
    @GetMapping("/paged")
    public Page<ReviewItemResponse> listPaged(@PathVariable Long listingId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(defaultValue = "createdAt,desc") String sort,
                                              HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        return reviewService.listPaged(listingId, anonId, page, size, sort);
    }

    // ✅ 평점 요약 (평균/개수)
    // 예: /api/listings/2/reviews/summary  -> { "average": 4.5, "count": 12 }
    @GetMapping("/summary")
    public RatingSummaryResponse summary(@PathVariable Long listingId) {
        return reviewService.ratingSummary(listingId);
    }
}
