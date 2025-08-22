package com.hackerton.hackerton2025.Controller;



import com.hackerton.hackerton2025.Dto.AskRequest;
import com.hackerton.hackerton2025.Dto.AskResponse;
import com.hackerton.hackerton2025.Service.OpenAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor



public class AiController {

    private final OpenAiService ai;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            return ai.health();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "openai_unreachable", "detail", e.getMessage()));
        }
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@Valid @RequestBody AskRequest req) {
        try {
            String text = ai.ask(req.getQuestion(), req.getSystem());
            return ResponseEntity.ok(new AskResponse(text));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "failed_to_ask_openai", "detail", e.getMessage()));
        }
    }
}