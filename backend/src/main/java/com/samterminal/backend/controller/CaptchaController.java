package com.samterminal.backend.controller;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.dto.AltchaVerifyRequest;
import com.samterminal.backend.exception.ApiException;
import com.samterminal.backend.service.AltchaService;
import com.samterminal.backend.service.EmailIpBanService;
import com.samterminal.backend.service.RateLimitService;
import com.samterminal.backend.service.RequestIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/captcha/altcha")
public class CaptchaController {
    private final AltchaService altchaService;
    private final RateLimitService rateLimitService;
    private final EmailVerificationProperties properties;
    private final EmailIpBanService ipBanService;
    private final RequestIpResolver ipResolver;

    public CaptchaController(AltchaService altchaService,
                             RateLimitService rateLimitService,
                             EmailVerificationProperties properties,
                             EmailIpBanService ipBanService,
                             RequestIpResolver ipResolver) {
        this.altchaService = altchaService;
        this.rateLimitService = rateLimitService;
        this.properties = properties;
        this.ipBanService = ipBanService;
        this.ipResolver = ipResolver;
    }

    @GetMapping("/challenge")
    public ResponseEntity<?> challenge(HttpServletRequest request) {
        String ip = ipResolver.resolve(request);
        ipBanService.assertNotBanned(ip);
        boolean allowed = rateLimitService.tryConsume("altcha:challenge:" + ip,
                properties.getRateLimit().getChallengePerMinute(), Duration.ofMinutes(1));
        if (!allowed) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁");
        }
        return ResponseEntity.ok(altchaService.fetchChallenge(ip));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody AltchaVerifyRequest body, HttpServletRequest request) {
        String ip = ipResolver.resolve(request);
        ipBanService.assertNotBanned(ip);
        boolean allowed = rateLimitService.tryConsume("altcha:verify:" + ip,
                properties.getRateLimit().getVerifyPerMinute(), Duration.ofMinutes(1));
        if (!allowed) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁");
        }
        boolean verified = altchaService.verifyPayload(body.getPayload(), ip);
        return ResponseEntity.ok(Map.of("verified", verified));
    }
}
