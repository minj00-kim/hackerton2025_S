package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.PostRequest;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Entity.Post;
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

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository; // ⭐ 주입
    private final KakaoGeoService kakaoGeoService;   // ⭐ 주소 → 좌표

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
                avgRating // ⭐ 추가
        );
    }
}
