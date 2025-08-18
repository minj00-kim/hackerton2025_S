package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.PostRequest;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Security.GuestCookieFilter;
import com.hackerton.hackerton2025.Service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;


import com.hackerton.hackerton2025.Service.FileStorageService;

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

    // 전체 조회 (비페이지네이션; 필요 없으면 제거 가능)
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
}
