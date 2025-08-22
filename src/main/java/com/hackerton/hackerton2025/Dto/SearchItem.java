package com.hackerton.hackerton2025.Dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 단일 검색 결과 아이템 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class SearchItem {

    private String title;
    private String url;
    private String snippet;
}
