// src/main/java/com/hackerton/hackerton2025/Service/PostService.java
package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.PostDetailResponse;
import com.hackerton.hackerton2025.Dto.PostRequest;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Dto.ReviewItemResponse;
import com.hackerton.hackerton2025.Entity.DealType;
import com.hackerton.hackerton2025.Entity.ListingStatus;
import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Repository.FavoriteRepository;
import com.hackerton.hackerton2025.Repository.PostRepository;
import com.hackerton.hackerton2025.Repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final KakaoGeoService kakaoGeoService;        // 주소 → 좌표
    private final KakaoRegionService kakaoRegionService;  // 좌표 → 시/구/동(코드)
    private final FavoriteRepository favoriteRepository;
    private final KakaoPlacesService kakaoPlacesService;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 등록 */
    public PostResponse createPost(Long ownerId, PostRequest request) {
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다.");
        validatePricing(request.getDealType(), request);

        var latLng = kakaoGeoService.geocode(request.getAddress())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "주소 결과 없음"));
        var reg = kakaoRegionService.coord2region(latLng.lat(), latLng.lng()).orElse(null);

        Post post = Post.builder()
                .ownerId(ownerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .address(request.getAddress())
                .latitude(latLng.lat())
                .longitude(latLng.lng())
                .category(request.getCategory())
                .imageUrls(safeUrls(request.getImageUrls()))
                // 부동산 필드
                .dealType(request.getDealType())
                .price(request.getPrice())
                .deposit(request.getDeposit())
                .rentMonthly(request.getRentMonthly())
                .maintenanceFee(request.getMaintenanceFee())
                .areaM2(request.getAreaM2())
                // 지역 필드
                .sido(     reg == null ? null : reg.getSido())
                .sigungu(  reg == null ? null : reg.getSigungu())
                .dong(     reg == null ? null : reg.getDong())
                .sidoCode( reg == null ? null : reg.getSidoCode())
                .sggCode(  reg == null ? null : reg.getSggCode())
                .dongCode( reg == null ? null : reg.getDongCode())
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
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(this::toResponse).toList();
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

    /** 수정 - 본인 소유만 가능 (주소 변경 시 좌표/지역 재계산) */
    public PostResponse updatePost(Long ownerId, Long id, PostRequest request) {
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다.");

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!ownerId.equals(post.getOwnerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글만 수정할 수 있습니다.");

        validatePricing(request.getDealType(), request);

        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setAddress(request.getAddress());
        post.setCategory(request.getCategory());
        post.setImageUrls(safeUrls(request.getImageUrls()));
        // 부동산 필드
        post.setDealType(request.getDealType());
        post.setPrice(request.getPrice());
        post.setDeposit(request.getDeposit());
        post.setRentMonthly(request.getRentMonthly());
        post.setMaintenanceFee(request.getMaintenanceFee());
        post.setAreaM2(request.getAreaM2());

        var latLng = kakaoGeoService.geocode(request.getAddress())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "주소 결과 없음"));
        post.setLatitude(latLng.lat());
        post.setLongitude(latLng.lng());

        kakaoRegionService.coord2region(latLng.lat(), latLng.lng()).ifPresent(r -> {
            post.setSido(r.getSido());
            post.setSigungu(r.getSigungu());
            post.setDong(r.getDong());
            post.setSidoCode(r.getSidoCode());
            post.setSggCode(r.getSggCode());
            post.setDongCode(r.getDongCode());
        });

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

    /** 상세(조회수 +1 포함) */
    @Transactional
    public PostDetailResponse getPostDetail(Long viewerId, Long postId) {
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        // 조회수 증가
        postRepository.incViews(postId);
        postRepository.save(p);

        PostResponse post = toResponse(p);

        boolean favorite = viewerId != null
                && favoriteRepository.existsByUserIdAndPost_Id(viewerId, postId);
        long reviewCount = reviewRepository.countByPost_Id(postId);

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

    /** (진단용) 특정 포스트 기준, 반경 내 카카오 그룹코드 집계 */
    public Map<String,Integer> nearbyGroupCounts(Long postId, int radiusM, String[] groupCodes) {
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        if (p.getLatitude() == null || p.getLongitude() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "좌표 없음");

        Map<String,Integer> out = new LinkedHashMap<>();
        for (String g : groupCodes) {
            int cnt = kakaoPlacesService.countByGroup(p.getLatitude(), p.getLongitude(), g.trim(), radiusM);
            out.put(g.trim(), cnt);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> countCategories() {
        return postRepository.countByCategory().stream()
                .map(v -> Map.<String,Object>of("name",  v.getCategory(), "count", v.getCnt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listBySgg(String sggCode, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findBySggCodeOrderByCreatedAtDesc(sggCode, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listByDong(String dongCode, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByDongCodeOrderByCreatedAtDesc(dongCode, pageable)
                .map(this::toResponse);
    }

    // ==== 검색(거래유형 + 정렬) ====
    @Transactional(readOnly = true)
    public Page<PostResponse> search(DealType dealType, String sort, int page, int size) {
        Sort s = buildSort(sort, dealType);
        Pageable pageable = PageRequest.of(page, size, s);

        Page<Post> result = (dealType == null)
                ? postRepository.findAll(pageable)
                : postRepository.findByDealType(dealType, pageable);

        return result.map(this::toResponse);
    }

    /** sort = latest | popular | priceAsc | priceDesc | areaAsc | areaDesc */
    private Sort buildSort(String sort, DealType dealType) {
        if (sort == null || sort.isBlank() || "latest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Order.desc("createdAt")); // 기본: 최신순
        }
        return switch (sort) {
            case "priceAsc"  -> (dealType == DealType.MONTHLY)
                    ? Sort.by(Sort.Order.asc("rentMonthly"), Sort.Order.asc("deposit"))
                    : Sort.by(Sort.Order.asc("price"));
            case "priceDesc" -> (dealType == DealType.MONTHLY)
                    ? Sort.by(Sort.Order.desc("rentMonthly"), Sort.Order.desc("deposit"))
                    : Sort.by(Sort.Order.desc("price"));
            case "areaAsc"   -> Sort.by(Sort.Order.asc("areaM2"));
            case "areaDesc"  -> Sort.by(Sort.Order.desc("areaM2"));
            case "popular"   -> Sort.by(
                    Sort.Order.desc("favCount"),
                    Sort.Order.desc("views"),
                    Sort.Order.desc("createdAt"));
            default -> Sort.by(Sort.Order.desc("createdAt"));
        };
    }

    /** 유효성 검사 */
    private void validatePricing(DealType type, PostRequest r) {
        switch (type) {
            case SALE -> {
                if (r.getPrice() == null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "매매가(price)가 필요합니다.");
            }
            case JEONSE -> {
                if (r.getPrice() == null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "전세보증금(price)가 필요합니다.");
            }
            case MONTHLY -> {
                if (r.getRentMonthly() == null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "월세(rentMonthly)가 필요합니다.");
                if (r.getDeposit() == null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보증금(deposit)이 필요합니다.");
            }
        }
        if (r.getAreaM2() == null || r.getAreaM2() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "면적(areaM2)이 필요합니다.");
    }

    /** Entity → Response */
    private PostResponse toResponse(Post post) {
        String created = (post.getCreatedAt() != null) ? post.getCreatedAt().format(TS_FMT) : null;

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
                post.getImageUrls(),
                post.getStatus().name(),

                // ===== 부동산 필드 =====
                post.getDealType(),
                post.getPrice(),
                post.getDeposit(),
                post.getRentMonthly(),
                post.getMaintenanceFee(),
                post.getAreaM2(),

                // ===== 추가: 조회/찜 =====
                post.getViews(),
                post.getFavCount()
        );
    }

    /** 유틸 */
    private List<String> safeUrls(List<String> urls) {
        if (urls == null) return Collections.emptyList();
        return urls.stream().filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
    }

    // ===== 상태 변경(문자열 버전) =====
    public PostResponse updateStatus(Long ownerId, Long id, String statusRaw) {
        if (ownerId == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다.");

        ListingStatus status;
        try {
            status = ListingStatus.valueOf(statusRaw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status 값은 AVAILABLE/RESERVED/SOLD 이어야 합니다.");
        }
        return updateStatus(ownerId, id, status); // 아래 enum 버전 재사용
    }

    // ===== 상태 변경(Enum 버전) — 컨트롤러가 enum을 넘겨도 대응 가능합니다.
    public PostResponse updateStatus(Long ownerId, Long id, ListingStatus status) {
        if (ownerId == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키가 없습니다.");

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!ownerId.equals(post.getOwnerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글만 수정할 수 있습니다.");

        post.setStatus(status);
        return toResponse(postRepository.save(post));
    }
}
