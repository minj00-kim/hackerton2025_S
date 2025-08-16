package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Dto.PostRequest;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Entity.User;
import com.hackerton.hackerton2025.Repository.PostRepository;
import com.hackerton.hackerton2025.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 등록
    public PostResponse createPost(PostRequest request) {
        User owner = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        Post post = Post.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .category(request.getCategory())
                .owner(owner)
                .build();

        Post saved = postRepository.save(post);

        return toResponse(saved);
    }

    // 조회 (단건)
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        return toResponse(post);
    }

    // 조회 (전체)
    public List<PostResponse> getAllPosts() {
        return postRepository.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 수정
    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setAddress(request.getAddress());
        post.setLatitude(request.getLatitude());
        post.setLongitude(request.getLongitude());
        post.setCategory(request.getCategory());

        Post updated = postRepository.save(post);
        return toResponse(updated);
    }

    // 삭제
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                post.getAddress(),
                post.getLatitude(),
                post.getLongitude(),
                post.getCategory(),
                post.getOwner().getId()
        );
    }
}
