package com.hackerton.hackerton2025.Controller;


import com.hackerton.hackerton2025.Service.AiProxyService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Validated
public class AiController {

    private final AiProxyService ai;

    // Node AI 서버 헬스체크 프록시
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ai.health();
    }

    // 지오코딩: GET /api/ai/geocode?address=서산시%20호수공원
    @GetMapping("/geocode")
    public ResponseEntity<String> geocode(@RequestParam @NotBlank String address) {
        return ai.geocode(address);
    }

    // 추천: POST /api/ai/recommend (본문: { listings: [...], options: {...} })
    @PostMapping("/recommend")
    public ResponseEntity<String> recommend(@RequestBody String body) {
        return ai.recommend(body);
    }

    // 시뮬레이터: POST /api/ai/simulate (본문: { ... })
    @PostMapping("/simulate")
    public ResponseEntity<String> simulate(@RequestBody String body) {
        return ai.simulate(body);
    }

    // 지역 비교: GET /api/ai/compare?a=부춘동&b=동문1동
    @GetMapping("/compare")
    public ResponseEntity<String> compare(
            @RequestParam("a") @NotBlank String a,
            @RequestParam("b") @NotBlank String b
    ) {
        return ai.compare(a, b);
    }
}