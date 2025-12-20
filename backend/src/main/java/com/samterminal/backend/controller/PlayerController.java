package com.samterminal.backend.controller;

import com.samterminal.backend.dto.GameStateDto;
import com.samterminal.backend.entity.Location;
import com.samterminal.backend.entity.UserLocationUnlock;
import com.samterminal.backend.repository.LocationRepository;
import com.samterminal.backend.repository.UserLocationUnlockRepository;
import com.samterminal.backend.service.GameService;
import com.samterminal.backend.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final GameService gameService;
    private final JwtService jwtService;
    private final UserLocationUnlockRepository unlockRepository;
    private final LocationRepository locationRepository;

    public PlayerController(GameService gameService, JwtService jwtService,
                            UserLocationUnlockRepository unlockRepository,
                            LocationRepository locationRepository) {
        this.gameService = gameService;
        this.jwtService = jwtService;
        this.unlockRepository = unlockRepository;
        this.locationRepository = locationRepository;
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> inventory(HttpServletRequest request) {
        GameStateDto dto = gameService.getState(resolveUser(request));
        return ResponseEntity.ok(dto.getItems());
    }

    @GetMapping("/memories")
    public ResponseEntity<?> memories(HttpServletRequest request) {
        GameStateDto dto = gameService.getState(resolveUser(request));
        return ResponseEntity.ok(dto.getMemories());
    }

    @GetMapping("/progress")
    public ResponseEntity<?> progress(HttpServletRequest request) {
        String username = resolveUser(request);
        var user = gameService.getOrCreateUser(username);
        var unlocks = unlockRepository.findByUser(user).stream()
                .map(UserLocationUnlock::getLocation)
                .map(Location::getCode)
                .toList();
        return ResponseEntity.ok(unlocks);
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
