package com.samterminal.backend.controller;

import com.samterminal.backend.dto.AuthRequest;
import com.samterminal.backend.service.AuthService;
import com.samterminal.backend.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AppUserRepository userRepository;

    public AuthController(AuthService authService, AppUserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        String token = authService.login(request);
        String role = userRepository.findByUsername(request.getUsername())
                .map(u -> u.getRole().name())
                .orElse("USER");
        return ResponseEntity.ok(Map.of("token", token, "username", request.getUsername(), "role", role));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        String token = authService.register(request);
        return ResponseEntity.ok(Map.of("token", token, "username", request.getUsername(), "role", "USER"));
    }
}
