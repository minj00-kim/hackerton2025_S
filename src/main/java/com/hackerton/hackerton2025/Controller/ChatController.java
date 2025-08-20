package com.hackerton.hackerton2025.Controller;
//질문>GPT검색>답변
import com.hackerton.hackerton2025.Dto.ChatRequest;
import com.hackerton.hackerton2025.Dto.ChatResponse;
import com.hackerton.hackerton2025.Dto.SearchDto.SearchItem;
import com.hackerton.hackerton2025.Dto.SearchDto.SearchResponse;
import com.hackerton.hackerton2025.Service.OpenAiService;
// ⬇️ SearchClient를 Client 서브패키지로 옮겼으니 여기 경로로 import!
import com.hackerton.hackerton2025.Service.Client.SearchClient;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SearchClient searchClient;
    private final OpenAiService openAi;

    @PostMapping("/ask")
    public ChatResponse ask(@RequestBody ChatRequest req) {
        int n = (req.getSearchCount() > 0 ? req.getSearchCount() : 5);

        List<SearchItem> sources = List.of();
        if (req.isEnableSearch()) {
            SearchResponse s = searchClient.search(req.getQuestion(), n);
            sources = s.getItems();
        }

        String answer = openAi.answerWithContext(req.getQuestion(), sources);
        return ChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .build();
    }



}
