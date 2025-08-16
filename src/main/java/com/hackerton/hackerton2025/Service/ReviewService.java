package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.ReviewRequest;
import com.hackerton.hackerton2025.Dto.ReviewResponse;
import com.hackerton.hackerton2025.Entity.Listing;
import com.hackerton.hackerton2025.Entity.Review;
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

        Review r = Review.builder()
                .listing(listing)
                .userId(userId)
                .rating(req.rating())
                .comment(req.comment())
                .build();

        return reviewRepo.save(r).getId();
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