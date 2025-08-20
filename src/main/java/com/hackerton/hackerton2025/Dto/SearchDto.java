package com.hackerton.hackerton2025.Dto;


import lombok.*;

import java.util.List;


public class SearchDto {


    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SearchItem {
        private String title;
        private String url;
        private String snippet;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SearchResponse {
        private String provider;       // bing | google | serpapi ...
        private List<SearchItem> items;
    }

}
