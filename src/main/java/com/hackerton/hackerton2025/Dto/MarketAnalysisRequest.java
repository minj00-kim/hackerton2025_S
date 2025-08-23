package com.hackerton.hackerton2025.Dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record MarketAnalysisRequest(
        @NotBlank String address,
        @NotNull  @Size(min=1) List<String> analysisTypes,   // 체크된 항목만
        List<String> interestedCategories,                   // 없으면 빈 배열
        @NotBlank String budgetBracket,
        @NotBlank String experience,
        @Size(max=500) String targetAudience,
        @Min(200) @Max(2000) Integer radius,                 // 기본 600
        Double lat,                                          // 프론트가 주면 사용
        Double lon
) {}
