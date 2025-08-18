package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.RatingSummaryResponse;
import com.hackerton.hackerton2025.Dto.ReviewItemResponse;
import com.hackerton.hackerton2025.Dto.ReviewRequest;
import com.hackerton.hackerton2025.Dto.ReviewResponse;
import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Entity.Review;
import com.hackerton.hackerton2025.Repository.PostRepository;
import com.hackerton.hackerton2025.Repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final PostRepository postRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 작성 */
    public Long write(Long postId, Long userId, ReviewRequest req) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 매물입니다."));

        // 게시물당 1개 제한
        if (reviewRepo.existsByPost_IdAndUserId(postId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 게시물에 리뷰를 작성했습니다.");
        }

        Review r = Review.builder()
                .post(post)
                .userId(userId)
                .rating(req.rating())
                .comment(req.comment())
                .build();

        return reviewRepo.save(r).getId();
    }

    /** 수정 */
    public Long update(Long postId, Long reviewId, Long userId, ReviewRequest req) {
        Review r = reviewRepo.findByIdAndPost_IdAndUserId(reviewId, postId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 리뷰만 수정할 수 있습니다."));
        r.setRating(req.rating());
        r.setComment(req.comment());
        return r.getId();
    }

    /** 삭제 */
    public void delete(Long postId, Long reviewId, Long userId) {
        Review r = reviewRepo.findByIdAndPost_IdAndUserId(reviewId, postId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 리뷰만 삭제할 수 있습니다."));
        reviewRepo.delete(r);
    }

    /** 목록(전체) — ReviewResponse 사용(userId 포함) */
    @Transactional(readOnly = true)
    public List<ReviewResponse> list(Long postId) {
        return reviewRepo.findByPost_IdOrderByCreatedAtDesc(postId)
                .stream()
                .map(r -> new ReviewResponse(
                        r.getId(),
                        r.getUserId(),
                        r.getRating(),
                        r.getComment(),
                        fmt(r.getCreatedAt())
                ))
                .toList();
    }

    /** 페이지네이션 + 내가 쓴 리뷰 여부 — ReviewItemResponse 사용(userId 숨김, mine 표시) */
    @Transactional(readOnly = true)
    public Page<ReviewItemResponse> listPaged(Long postId, Long currentUserId,
                                              int page, int size, String sort) {
        String order = (sort == null || sort.isBlank()) ? "createdAt,desc" : sort;
        String[] s = order.split(",", 2);
        Sort.Direction dir = (s.length > 1) ? Sort.Direction.fromString(s[1]) : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, s[0]));

        return reviewRepo.findByPost_Id(postId, pageable)
                .map(r -> new ReviewItemResponse(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        fmt(r.getCreatedAt()),
                        currentUserId != null && currentUserId.equals(r.getUserId())
                ));
    }

    /** 평점 요약(평균/개수) */
    @Transactional(readOnly = true)
    public RatingSummaryResponse ratingSummary(Long postId) {
        Double avg = reviewRepo.avgRating(postId);        // null 가능
        long cnt = reviewRepo.countByPost_Id(postId);
        double rounded = (avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0);
        return new RatingSummaryResponse(rounded, cnt);
    }

    // ─────────────────────────────────────────────────────────────

    private String fmt(LocalDateTime t) {
        return (t == null) ? null : t.format(FMT);
    }
}
