package com.hackerton.hackerton2025.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ChatRequest {

    private String question;   // 사용자가 입력한 질문
    private boolean enableSearch; // true면 서버가 웹검색도 수행
    private int searchCount;

}
