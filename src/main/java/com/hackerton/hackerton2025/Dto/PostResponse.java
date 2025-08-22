// src/main/java/com/hackerton/hackerton2025/Dto/PostResponse.java
package com.hackerton.hackerton2025.Dto;

import com.hackerton.hackerton2025.Entity.DealType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
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

    // ===== 부동산 필드 =====
    private DealType dealType;   // SALE/JEONSE/MONTHLY
    private Long price;          // SALE/JEONSE 공용
    private Long deposit;        // MONTHLY 전용
    private Long rentMonthly;    // MONTHLY 전용
    private Long maintenanceFee; // 관리비(원/월)
    private Double areaM2;       // 실면적 m²

    // ===== 추가: 조회/찜 =====
    private long views;          // 조회수
    private long favCount;       // 찜수

    private String contactPhone;     // 등록자 연락처
    private Boolean moveInImmediate; // 즉시 입주 여부
    private String moveInDate;       // yyyy-MM-dd 문자열
    private String moveInText;       // "즉시 입주" 또는 "yyyy년 M월 d일 입주 가능"
}
