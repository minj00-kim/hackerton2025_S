package com.hackerton.hackerton2025.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private String category;
    private Long ownerId;      // ↓ 2번 참고해서 유지/대체 결정
    private String createdAt;  // "yyyy-MM-dd HH:mm" 같은 포맷 권장
    private Double avgRating;
}

