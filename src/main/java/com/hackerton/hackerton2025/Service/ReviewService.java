package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.ReviewRequest;
import com.hackerton.hackerton2025.Dto.ReviewResponse;
import com.hackerton.hackerton2025.Entity.Listing;
import com.hackerton.hackerton2025.Entity.Review;
import com.hackerton.hackerton2025.Exception.DuplicateReviewException;
import com.hackerton.hackerton2025.Repository.ListingRepository;
import com.hackerton.hackerton2025.Repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
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

    public Long write(Long listingId, Long userId, ReviewRequest req) {
        Listing listing = listingRepo.findById(listingId).orElseThrow();

        // 게시물당 1개 제한
        if (reviewRepo.existsByListing_IdAndUserId(listingId, userId)) {
            throw new DuplicateReviewException();
        }

        Review r = Review.builder()
                .listing(listing)
                .userId(userId)
                .rating(req.rating())
                .comment(req.comment())
                .build();

        return reviewRepo.save(r).getId();
    }

    public Long update(Long listingId, Long reviewId, Long userId, ReviewRequest req) {
        Review r = reviewRepo.findByIdAndListing_IdAndUserId(reviewId, listingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 없거나 권한이 없습니다"));
        r.setRating(req.rating());
        r.setComment(req.comment());
        // updatedAt 컬럼이 없으니 필요하면 엔티티에 추가해서 갱신해
        return r.getId();
    }

    public void delete(Long listingId, Long reviewId, Long userId) {
        Review r = reviewRepo.findByIdAndListing_IdAndUserId(reviewId, listingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 없거나 권한이 없습니다"));
        reviewRepo.delete(r);
    }

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
}