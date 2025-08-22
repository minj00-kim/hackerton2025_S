package com.hackerton.hackerton2025.Dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder

public class TopDataDto {


    private Period period;
    private Section total;
    private Section latest;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Period {
        private String start;
        private String end;
        private String latest;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Section {
        private Item top_count;
        private Item top_amount;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private String name;
        private Long count;   // top_count일 때 사용
        private Long amount;  // top_amount일 때 사용
    }

}
