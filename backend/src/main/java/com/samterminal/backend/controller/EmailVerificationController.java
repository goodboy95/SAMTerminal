package com.samterminal.backend.controller;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.dto.EmailCodeSendRequest;
import com.samterminal.backend.dto.EmailCodeVerifyRequest;
import com.samterminal.backend.dto.EmailSendStatusResponse;
import com.samterminal.backend.exception.ApiException;
import com.samterminal.backend.service.EmailVerificationService;
import com.samterminal.backend.service.RateLimitService;
import com.samterminal.backend.service.RequestIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth/register/email-code")
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;
    private final RateLimitService rateLimitService;
    private final EmailVerificationProperties properties;
    private final RequestIpResolver ipResolver;

    public EmailVerificationController(EmailVerificationService emailVerificationService,
                                       RateLimitService rateLimitService,
                                       EmailVerificationProperties properties,
                                       RequestIpResolver ipResolver) {
        this.emailVerificationService = emailVerificationService;
        this.rateLimitService = rateLimitService;
        this.properties = properties;
        this.ipResolver = ipResolver;
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@Valid @RequestBody EmailCodeSendRequest request, HttpServletRequest httpRequest) {
        String ip = ipResolver.resolve(httpRequest);
        boolean allowed = rateLimitService.tryConsume("email:send:" + ip,
                properties.getRateLimit().getSendPerMinute(), Duration.ofMinutes(1));
        if (!allowed) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "发送过于频繁");
        }
        var response = emailVerificationService.sendRegisterCode(
                request.getUsername(), request.getEmail(), ip, request.getAltchaPayload());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody EmailCodeVerifyRequest request, HttpServletRequest httpRequest) {
        String ip = ipResolver.resolve(httpRequest);
        boolean allowed = rateLimitService.tryConsume("email:verify:" + ip,
                properties.getRateLimit().getVerifyCodePerMinute(), Duration.ofMinutes(1));
        if (!allowed) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁");
        }
        var response = emailVerificationService.verifyCode(
                request.getEmailRequestId(), request.getEmail(), request.getEmailCode(), ip);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/send-status")
    public ResponseEntity<?> sendStatus(@RequestParam("requestId") String requestId) {
        var status = emailVerificationService.getSendStatus(requestId);
        if (status == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "发送记录不存在"));
        }
        return ResponseEntity.ok(EmailSendStatusResponse.builder()
                .status(status.name())
                .lastError(emailVerificationService.getSendLastError(requestId))
                .build());
    }
}
