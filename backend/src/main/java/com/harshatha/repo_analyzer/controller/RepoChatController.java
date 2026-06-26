package com.harshatha.repo_analyzer.controller;

import com.harshatha.repo_analyzer.service.GroqChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepoChatController {

    private final GroqChatService groqChatService;

    public RepoChatController(GroqChatService groqChatService) {
        this.groqChatService = groqChatService;
    }

    @PostMapping("/{id}/chat")
    public ResponseEntity<?> queryCodebase(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload
    ) {
        String question = payload.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question parameter required"));
        }

        String aiResponse = groqChatService.askCodebase(id, question);
        return ResponseEntity.ok(Map.of("response", aiResponse));
    }
}