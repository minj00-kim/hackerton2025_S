package com.hackerton.hackerton2025.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

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
    private Long ownerId;     // 소유자(익명 쿠키 기준)
    private String createdAt; // "yyyy-MM-dd HH:mm"
    private double avgRating; // 소수 1자리 반올림 값
    private List<String> imageUrls; // 업로드 이미지 URL 목록
    private String status;
}
