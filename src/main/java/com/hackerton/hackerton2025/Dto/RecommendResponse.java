package com.hackerton.hackerton2025.Dto;



import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor


public class RecommendResponse {

    private Long id;
    private String category;
    private Double score;
    private String reason;
    private Long postId;
}
