package com.hackerton.hackerton2025.Controller;



import com.hackerton.hackerton2025.Dto.SearchResponse;   // ✅ DTO 임포트
import com.hackerton.hackerton2025.Service.Client.WebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api/websearch")
    @RequiredArgsConstructor
    public class WebSearchController {private final WebClient searchClient;

        @GetMapping
        public SearchResponse search(
                @RequestParam("q") String q,
                @RequestParam(value = "count", defaultValue = "5") int count
        ) {
            return searchClient.search(q, count);
        }

}



