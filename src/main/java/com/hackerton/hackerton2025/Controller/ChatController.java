package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Dto.AskRequest;
import com.hackerton.hackerton2025.Dto.AskResponse;
import com.hackerton.hackerton2025.Service.OpenAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor

public class ChatController {

    private final OpenAiService openAi;

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest req) {
        String answer = openAi.ask(req.getQuestion(), req.getSystem());
        return new AskResponse(answer);
    }

}
