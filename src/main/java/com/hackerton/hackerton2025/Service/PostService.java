package com.hackerton.hackerton2025.Service;



import com.hackerton.hackerton2025.Dto.PostRequest;
import com.hackerton.hackerton2025.Dto.PostResponse;
import com.hackerton.hackerton2025.Entity.Post;
import com.hackerton.hackerton2025.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {


    private final PostRepository postRepository;

    public PostResponse createPost(PostRequest request) {
        Post post = new Post(request.getUser(), request.getLocation(), request.getPrice());
        Post saved = postRepository.save(post);
        return new PostResponse(saved.getUser(), saved.getLocation(), saved.getPrice());
    }


}
