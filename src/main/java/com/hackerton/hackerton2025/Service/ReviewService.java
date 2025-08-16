// src/main/java/com/hackerton/hackerton2025/Service/ReviewService.java
package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.RatingSummaryResponse;
import com.hackerton.hackerton2025.Dto.ReviewItemResponse;
import com.hackerton.hackerton2025.Dto.ReviewRequest;
import com.hackerton.hackerton2025.Dto.ReviewResponse;
import com.hackerton.hackerton2025.Entity.Listing;
import com.hackerton.hackerton2025.Entity.Review;
import com.hackerton.hackerton2025.Repository.ListingRepository;
import com.hackerton.hackerton2025.Repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final ListingRepository listingRepo;

    // 작성
    public Long write(Long listingId, Long userId, ReviewRequest req) {
        Listing listing = listingRepo.findById(listingId).orElseThrow();

        // 게시물당 1개 제한 (이미 구현해둔 경우 생략 가능)
        if (reviewRepo.existsByListing_IdAndUserId(listingId, userId)) {
            throw new IllegalStateException("이미 이 게시물에 리뷰를 작성했습니다.");
        }

        Review r = Review.builder()
                .listing(listing)
                .userId(userId)
                .rating(req.rating())
                .comment(req.comment())
                .build();

        return reviewRepo.save(r).getId();
    }

    // 수정
    public Long update(Long listingId, Long reviewId, Long userId, ReviewRequest req) {
        Review r = reviewRepo.findByIdAndListing_IdAndUserId(reviewId, listingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 없거나 권한이 없습니다"));
        r.setRating(req.rating());
        r.setComment(req.comment());
        return r.getId();
    }

    // 삭제
    public void delete(Long listingId, Long reviewId, Long userId) {
        Review r = reviewRepo.findByIdAndListing_IdAndUserId(reviewId, listingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 없거나 권한이 없습니다"));
        reviewRepo.delete(r);
    }

    // 기존 목록(전체)
    @Transactional(readOnly = true)
    public List<ReviewResponse> list(Long listingId) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return reviewRepo.findByListing_IdOrderByCreatedAtDesc(listingId)
                .stream()
                .map(r -> new ReviewResponse(
                        r.getId(),
                        r.getUserId(),
                        r.getRating(),
                        r.getComment(),
                        r.getCreatedAt().format(f)
                ))
                .toList();
    }

    // ✅ 페이지네이션 + mine 플래그
    @Transactional(readOnly = true)
    public Page<ReviewItemResponse> listPaged(Long listingId, Long currentUserId,
                                              int page, int size, String sort) {
        String order = (sort == null || sort.isBlank()) ? "createdAt,desc" : sort;
        String[] s = order.split(",", 2);
        Sort.Direction dir = s.length > 1 ? Sort.Direction.fromString(s[1]) : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, s[0]));

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return reviewRepo.findByListing_Id(listingId, pageable)
                .map(r -> new ReviewItemResponse(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getCreatedAt().format(f),
                        currentUserId != null && currentUserId.equals(r.getUserId())
                ));
    }

    // ✅ 평점 요약(평균/개수)
    @Transactional(readOnly = true)
    public RatingSummaryResponse ratingSummary(Long listingId) {
        Double avg = reviewRepo.avgRating(listingId);          // null일 수 있음
        long cnt = reviewRepo.countByListing_Id(listingId);
        double rounded = (avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0); // 소수 1자리
        return new RatingSummaryResponse(rounded, cnt);
    }
}
