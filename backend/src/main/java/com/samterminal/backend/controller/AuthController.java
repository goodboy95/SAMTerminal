package com.samterminal.backend.controller;

import com.samterminal.backend.dto.AuthRequest;
import com.samterminal.backend.service.AuthService;
import com.samterminal.backend.service.RequestIpResolver;
import com.samterminal.backend.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AppUserRepository userRepository;
    private final RequestIpResolver ipResolver;

    public AuthController(AuthService authService, AppUserRepository userRepository, RequestIpResolver ipResolver) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.ipResolver = ipResolver;
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
    public ResponseEntity<?> register(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        String ip = ipResolver.resolve(httpRequest);
        String token = authService.register(request, ip);
        return ResponseEntity.ok(Map.of("token", token, "username", request.getUsername(), "role", "USER"));
    }
}
