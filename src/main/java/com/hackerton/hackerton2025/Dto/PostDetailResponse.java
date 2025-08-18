package com.hackerton.hackerton2025.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PostDetailResponse {
    private PostResponse post;        // 기존 상세 데이터
    private boolean favorite;         // 내가 찜했는지
    private long reviewCount;         // 리뷰 총 개수
    private List<ReviewItemResponse> reviews;  // 최신 N개
}