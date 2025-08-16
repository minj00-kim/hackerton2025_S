package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.PostRequest;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 등록
    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody @Valid PostRequest request) {
        return ResponseEntity.ok(postService.createPost(request));
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // 수정
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id, @RequestBody @Valid PostRequest request) {
        return ResponseEntity.ok(postService.updatePost(id, request));
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok("게시글이 삭제되었습니다.");
    }
}
