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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/{postId}/reviews") // ✅ Post 기준 경로
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성 (익명 쿠키 기반)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long write(@PathVariable Long postId,
                      @RequestBody @Valid ReviewRequest req,
                      HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        if (anonId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        return reviewService.write(postId, anonId, req);
    }

    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public Long update(@PathVariable Long postId,
                       @PathVariable Long reviewId,
                       @RequestBody @Valid ReviewRequest req,
                       HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        if (anonId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        return reviewService.update(postId, reviewId, anonId, req);
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long postId,
                       @PathVariable Long reviewId,
                       HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        if (anonId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        reviewService.delete(postId, reviewId, anonId);
    }

    // 리뷰 목록 (전체)
    @GetMapping
    public List<ReviewResponse> list(@PathVariable Long postId) {
        return reviewService.list(postId);
    }

    // 리뷰 목록 (페이지네이션 + 내가 쓴 리뷰 여부)
    // 예: /posts/2/reviews/paged?page=0&size=10&sort=createdAt,desc
    @GetMapping("/paged")
    public Page<ReviewItemResponse> listPaged(@PathVariable Long postId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(defaultValue = "createdAt,desc") String sort,
                                              HttpServletRequest request) {
        Long anonId = (Long) request.getAttribute(GuestCookieFilter.ATTR);
        return reviewService.listPaged(postId, anonId, page, size, sort);
    }

    // 평점 요약 (평균/개수)
    // 예: /posts/2/reviews/summary  -> { "average": 4.5, "count": 12 }
    @GetMapping("/summary")
    public RatingSummaryResponse summary(@PathVariable Long postId) {
        return reviewService.ratingSummary(postId);
    }
}
