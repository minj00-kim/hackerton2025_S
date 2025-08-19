package com.hackerton.hackerton2025.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegionBucketResponse {
    private String code;   // sgg_code
    private String name;   // 시군구명
    private long count;    // 매물 수
    private Double lat;    // 평균 위도(마커 중심용)
    private Double lng;    // 평균 경도
}
