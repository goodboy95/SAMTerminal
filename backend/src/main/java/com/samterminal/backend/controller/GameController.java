package com.samterminal.backend.controller;

import com.samterminal.backend.dto.ChatRequest;
import com.samterminal.backend.dto.ChatResponse;
import com.samterminal.backend.dto.GameStateDto;
import com.samterminal.backend.dto.MemoryRecallRequest;
import com.samterminal.backend.service.GameService;
import com.samterminal.backend.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;
    private final JwtService jwtService;

    public GameController(GameService gameService, JwtService jwtService) {
        this.gameService = gameService;
        this.jwtService = jwtService;
    }

    @GetMapping("/status")
    public ResponseEntity<GameStateDto> status(HttpServletRequest request) {
        String username = resolveUser(request);
        return ResponseEntity.ok(gameService.getState(username));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        String username = resolveUser(request);
        return ResponseEntity.ok(gameService.handleChat(username, chatRequest.getMessage()));
    }

    @PostMapping("/memory/recall")
    public ResponseEntity<ChatResponse> recall(@RequestBody MemoryRecallRequest request, HttpServletRequest httpRequest) {
        String username = resolveUser(httpRequest);
        return ResponseEntity.ok(gameService.recallMemory(username, request.getMemoryId()));
    }

    private String resolveUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                return jwtService.extractUsername(header.substring(7));
            } catch (Exception ignored) {}
        }
        return "trailblazer";
    }
}
