package com.hackerton.hackerton2025.Dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ChatResponse {

    private String answer;                  // GPT가 만들어낸 최종 답
    private List<SearchDto.SearchItem> sources;       // 함께 보여줄 출처(정규화된 검색 결과)


}
