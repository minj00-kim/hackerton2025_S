package com.hackerton.hackerton2025.Dto;

public record ReviewItemResponse(
        Long id,
        Integer rating,
        String comment,
        String createdAt,
        boolean mine    // 현재 쿠키 사용자(anon_id)가 쓴 리뷰인지
) {}