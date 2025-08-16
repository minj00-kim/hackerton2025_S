package com.hackerton.hackerton2025.Controller;


import com.hackerton.hackerton2025.Dto.PostRequest;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor

public class PostController {


    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody @Valid PostRequest request) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity.ok(response);
    }

}
