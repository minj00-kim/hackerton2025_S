package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.SearchDto.SearchResponse;
import com.hackerton.hackerton2025.Service.Client.SearchClient;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/websearch")
@RequiredArgsConstructor
public class WebSearchController {

    private final SearchClient searchClient;  // 변수명 명확히

    @GetMapping
    public SearchResponse search(
            @RequestParam("q") String q,
            @RequestParam(value = "count", defaultValue = "5") int count
    ) {
        return searchClient.search(q, count);
    }
}



