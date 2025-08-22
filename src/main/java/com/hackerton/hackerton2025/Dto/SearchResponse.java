package com.hackerton.hackerton2025.Dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/** 검색 응답 래퍼 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class SearchResponse {

    private String provider;  // 예: "bing"

    @Builder.Default
    private List<SearchItem> items = Collections.emptyList();
}
