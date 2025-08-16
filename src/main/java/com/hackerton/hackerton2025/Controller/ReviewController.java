package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.ReviewRequest;
import com.hackerton.hackerton2025.Dto.ReviewResponse;
import com.hackerton.hackerton2025.Service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/listings/{listingId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성
    @PostMapping
    public Long write(@PathVariable Long listingId,
                      @RequestBody @Valid ReviewRequest req,
                      HttpSession session) {
        Long anonId = (Long) session.getAttribute("ANON_USER_ID");
        if (anonId == null) {
            anonId = ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE);
            session.setAttribute("ANON_USER_ID", anonId);
        }
        return reviewService.write(listingId, anonId, req);
    }

    // 리뷰 목록
    @GetMapping
    public List<ReviewResponse> list(@PathVariable Long listingId) {
        return reviewService.list(listingId);
    }
}
