// src/main/java/com/hackerton/hackerton2025/Service/PostService.java
package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.PostDetailResponse;
import com.hackerton.hackerton2025.Dto.PostRequest;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Dto.ReviewItemResponse;
import com.hackerton.hackerton2025.Entity.ListingStatus;
import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Repository.FavoriteRepository;
import com.hackerton.hackerton2025.Repository.PostRepository;
import com.hackerton.hackerton2025.Repository.ReviewRepository; // ⭐ 평균 별점 조회용
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hackerton.hackerton2025.Entity.ListingStatus;
import com.hackerton.hackerton2025.Dto.PostDetailResponse;
import com.hackerton.hackerton2025.Dto.ReviewItemResponse;
import com.hackerton.hackerton2025.Repository.FavoriteRepository;
import com.hackerton.hackerton2025.Entity.Review;
import java.util.Objects;

import java.util.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository; // ⭐ 주입
    private final KakaoGeoService kakaoGeoService;   // ⭐ 주소 → 좌표
    private final FavoriteRepository favoriteRepository;
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 등록 - ownerId는 컨트롤러에서 쿠키(anon_id)로 받아서 넘김 */
    public PostResponse createPost(Long ownerId, PostRequest request) {
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다.");

        // 주소로 좌표 조회 (요청의 lat/lng는 무시)
        var latLng = kakaoGeoService.geocode(request.getAddress())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "주소 결과 없음"));

        Post post = Post.builder()
                .ownerId(ownerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .address(request.getAddress())
                .latitude(latLng.lat())
                .longitude(latLng.lng())
                .category(request.getCategory())
                .imageUrls(safeUrls(request.getImageUrls()))   // ✅ 이미지 URL 저장
                .build();

        return toResponse(postRepository.save(post));
    }

    /** 조회(단건) */
    @Transactional(readOnly = true)
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        return toResponse(post);
    }

    /** 전체 조회(최신순) */
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts() {
        return postRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 내가 쓴 글 목록(페이지네이션, 최신순) */
    @Transactional(readOnly = true)
    public Page<PostResponse> myPosts(Long ownerId, int page, int size) {
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다.");
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByOwnerId(ownerId, pageable).map(this::toResponse);
    }

    /** 카테고리별 목록(페이지네이션, 최신순) */
    @Transactional(readOnly = true)
    public Page<PostResponse> listByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByCategory(category, pageable).map(this::toResponse);
    }

    /** 지도 바운딩박스 목록(페이지네이션, 최신순) */
    @Transactional(readOnly = true)
    public Page<PostResponse> listInBounds(double minLat, double maxLat, double minLng, double maxLng,
                                           int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository
                .findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLng, maxLng, pageable)
                .map(this::toResponse);
    }

    /** 수정 - 본인 소유만 가능 (주소 변경 시 좌표 재계산) */
    public PostResponse updatePost(Long ownerId, Long id, PostRequest request) {
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다.");

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!ownerId.equals(post.getOwnerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글만 수정할 수 있습니다.");

        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setAddress(request.getAddress());

        // 주소 기준 좌표 재조회 (요청의 lat/lng는 무시)
        var latLng = kakaoGeoService.geocode(request.getAddress())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "주소 결과 없음"));
        post.setLatitude(latLng.lat());
        post.setLongitude(latLng.lng());

        post.setCategory(request.getCategory());
        post.setImageUrls(safeUrls(request.getImageUrls())); // ✅ 이미지 URL 갱신

        return toResponse(postRepository.save(post));
    }

    /** 삭제 - 본인 소유만 가능 */
    public void deletePost(Long ownerId, Long id) {
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다.");

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!ownerId.equals(post.getOwnerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글만 삭제할 수 있습니다.");

        postRepository.delete(post);
    }

    /** Post -> PostResponse (+ avgRating 포함) */
    private PostResponse toResponse(Post post) {
        String created = (post.getCreatedAt() != null) ? post.getCreatedAt().format(TS_FMT) : null;

        // ⭐ 평균 별점 조회 (리뷰 없으면 null → 0.0), 소수 첫째자리 반올림
        Double avg = reviewRepository.avgRating(post.getId());
        double avgRating = (avg == null) ? 0.0 : Math.round(avg * 10.0) / 10.0;

        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                post.getAddress(),
                post.getLatitude(),
                post.getLongitude(),
                post.getCategory(),
                post.getOwnerId(),
                created,
                avgRating,
                post.getImageUrls(),  // ✅ 응답에 이미지 URL 포함
                post.getStatus().name()
        );
    }

    /** null/공백 제거 + 중복 제거 */
    private List<String> safeUrls(List<String> urls) {
        if (urls == null) return Collections.emptyList();
        return urls.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    public PostResponse updateStatus(Long ownerId, Long id, String statusRaw) {
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다.");

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!ownerId.equals(post.getOwnerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글만 수정할 수 있습니다.");

        ListingStatus status;
        try {
            status = ListingStatus.valueOf(statusRaw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 값은 AVAILABLE/RESERVED/SOLD 이어야 합니다.");
        }

        post.setStatus(status);
        return toResponse(postRepository.save(post));
    }
    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long viewerId, Long postId) {
        // 기존 PostResponse 재사용
        PostResponse post = getPost(postId);

        // ✅ 즐겨찾기 여부 (엔티티가 @ManyToOne Post post; 구조일 때 _Id 네이밍)
        boolean favorite = viewerId != null
                && favoriteRepository.existsByUserIdAndPost_Id(viewerId, postId);

        // ✅ 리뷰 개수
        long reviewCount = reviewRepository.countByPost_Id(postId);

        // ✅ 최신 10개 리뷰: findTop10... 대신 Pageable 사용 (이미 ReviewService에서 쓰던 방식)
        Pageable top10 = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        var latest = reviewRepository.findByPost_Id(postId, top10).getContent();

        var reviews = latest.stream()
                .map(r -> new ReviewItemResponse(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getCreatedAt() == null ? null : r.getCreatedAt().format(TS_FMT),
                        viewerId != null && Objects.equals(viewerId, r.getUserId())
                ))
                .toList();

        return new PostDetailResponse(post, favorite, reviewCount, reviews);
    }
}
