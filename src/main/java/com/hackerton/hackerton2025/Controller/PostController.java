package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.PostDetailResponse;
import com.hackerton.hackerton2025.Dto.PostRequest;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Dto.UpdateStatusRequest;
import com.hackerton.hackerton2025.Entity.DealType;
import com.hackerton.hackerton2025.Security.GuestCookieFilter;
import com.hackerton.hackerton2025.Service.FileStorageService;
import com.hackerton.hackerton2025.Service.PostService;
import com.hackerton.hackerton2025.Support.CategoryRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final FileStorageService fileStorageService;

    // 사진 등록
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadImages(
            @RequestPart("images") List<MultipartFile> images) {
        List<String> urls = fileStorageService.saveAll(images); // /uploads/2025/08/18/xxx.jpg
        return ResponseEntity.ok(urls);
    }

    // 등록 (쿠키로 소유자 식별)
    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody @Valid PostRequest request,
                                                   HttpServletRequest req) {
        Long ownerId = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        return ResponseEntity.ok(postService.createPost(ownerId, request));
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    // 전체 조회 (비페이지네이션)
    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // ✅ 내 글 목록 (페이지네이션, 최신순)
    @GetMapping("/my")
    public ResponseEntity<Page<PostResponse>> myPosts(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      HttpServletRequest req) {
        Long ownerId = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        return ResponseEntity.ok(postService.myPosts(ownerId, page, size));
    }

    // ✅ 카테고리별 목록 (페이지네이션, 최신순)
    @GetMapping("/by-category")
    public ResponseEntity<Page<PostResponse>> listByCategory(@RequestParam String category,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.listByCategory(category, page, size));
    }

    // ✅ 위경도 바운딩 박스 검색 (페이지네이션, 최신순)
    @GetMapping("/in-bounds")
    public ResponseEntity<Page<PostResponse>> listInBounds(@RequestParam double minLat,
                                                           @RequestParam double maxLat,
                                                           @RequestParam double minLng,
                                                           @RequestParam double maxLng,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.listInBounds(minLat, maxLat, minLng, maxLng, page, size));
    }

    // 수정 (본인 글만)
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id,
                                                   @RequestBody @Valid PostRequest request,
                                                   HttpServletRequest req) {
        Long ownerId = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        return ResponseEntity.ok(postService.updatePost(ownerId, id, request));
    }

    // 삭제 (본인 글만)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(@PathVariable Long id,
                                             HttpServletRequest req) {
        Long ownerId = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        postService.deletePost(ownerId, id);
        return ResponseEntity.ok("게시글이 삭제되었습니다.");
    }

    // 상태 변경
    @PatchMapping("/{id}/status")
    public ResponseEntity<PostResponse> updateStatus(@PathVariable Long id,
                                                     @RequestBody @Valid UpdateStatusRequest body,
                                                     HttpServletRequest req) {
        Long ownerId = (Long) req.getAttribute(GuestCookieFilter.ATTR);
        if (ownerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "익명 쿠키 없음");
        return ResponseEntity.ok(postService.updateStatus(ownerId, id, body.getStatus()));
    }

    // 상세(조회수 증가 + 찜/리뷰 포함)
    @GetMapping("/{id}/detail")
    public ResponseEntity<PostDetailResponse> getDetail(@PathVariable Long id,
                                                        HttpServletRequest req) {
        Long viewerId = (Long) req.getAttribute(GuestCookieFilter.ATTR); // 없으면 null
        return ResponseEntity.ok(postService.getPostDetail(viewerId, id));
    }

    // 카테고리 목록 + 개수
    @GetMapping("/categories")
    public ResponseEntity<List<Map<String,Object>>> categoriesWithCounts() {
        var rows = postService.countCategories();
        var countMap = rows.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> r.get("name").toString(),
                        r -> (Long) r.get("count")
                ));

        var out = CategoryRegistry.CATEGORIES.stream()
                .map(name -> {
                    long c = countMap.getOrDefault(name, 0L);
                    return java.util.Map.<String,Object>of("name", name, "count", c);
                })
                .toList();

        return ResponseEntity.ok(out);
    }

    // 특정 매물 기준 주변 업종 카운트
    @GetMapping("/{id}/nearby")
    public ResponseEntity<Map<String,Object>> nearby(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "500") int radius,
                                                     @RequestParam(defaultValue = "FD6,CE7,HP8,CS2") String cats) {
        String[] codes = Arrays.stream(cats.split(",")).map(String::trim).toArray(String[]::new);
        var counts = postService.nearbyGroupCounts(id, radius, codes);
        return ResponseEntity.ok(Map.of(
                "postId", id,
                "radius", radius,
                "counts", counts
        ));
    }

    // 거래유형 필터 + 정렬 검색
    @GetMapping("/search")
    public ResponseEntity<Page<PostResponse>> search(
            @RequestParam(required = false) DealType dealType,      // null이면 전체
            @RequestParam(defaultValue = "latest") String sort,     // latest|popular|priceAsc|priceDesc|areaAsc|areaDesc
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(postService.search(dealType, sort, page, size));
    }
}
