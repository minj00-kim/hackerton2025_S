package com.hackerton.hackerton2025.Dto;

public record RegionBubble(
        String code,   // sgg_code 또는 dong_code
        String name,   // 시군구명 또는 읍면동명
        long count,    // 매물 수
        double lat,    // 평균 위도(마커 표시용)
        double lng     // 평균 경도(마커 표시용)
) {}
