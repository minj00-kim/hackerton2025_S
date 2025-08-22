package com.hackerton.hackerton2025.Dto;

public record ReviewResponse(
        Long id,
        Long userId,
        Integer rating,
        String comment,
        String createdAt   // 서비스/컨트롤러에서 포맷팅해서 넣어줌
) {}


