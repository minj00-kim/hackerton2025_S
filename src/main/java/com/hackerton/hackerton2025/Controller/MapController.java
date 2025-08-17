package com.hackerton.hackerton2025.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController                // ← @Controller 지우고 이것만
@RequestMapping("/api/map")    // 클래스 프리픽스
public class MapController {

    // 최종 경로: GET /api/map/search?query=치킨
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String query) {
        // 일단 컴파일/동작 확인용 응답
        return Map.of("ok", true, "query", query);
    }


}
