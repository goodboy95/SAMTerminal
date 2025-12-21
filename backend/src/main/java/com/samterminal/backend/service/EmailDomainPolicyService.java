package com.samterminal.backend.service;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class EmailDomainPolicyService {
    private final EmailVerificationProperties properties;
    private final ResourceLoader resourceLoader;
    private final Set<String> disposableDomains = new HashSet<>();

    public EmailDomainPolicyService(EmailVerificationProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void loadDisposableDomains() {
        String path = properties.getDomainPolicy().getDisposableListPath();
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .forEach(line -> disposableDomains.add(line.toLowerCase(Locale.ROOT)));
            }
        } catch (Exception ex) {
            // ignore loading errors to avoid blocking startup
        }
    }

    public void validateEmail(String email) {
        if (!properties.getDomainPolicy().isEnabled()) {
            return;
        }
        String domain = extractDomain(email);
        if (domain == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "邮箱格式错误");
        }
        String lower = domain.toLowerCase(Locale.ROOT);
        var denylist = properties.getDomainPolicy().getDenylist().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        var allowlist = properties.getDomainPolicy().getAllowlist().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (denylist.stream().anyMatch(d -> d.equalsIgnoreCase(lower))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "邮箱不支持，请更换邮箱");
        }
        if (!allowlist.isEmpty()) {
            boolean allowed = allowlist.stream()
                    .anyMatch(d -> d.equalsIgnoreCase(lower));
            if (!allowed) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "邮箱不支持，请更换邮箱");
            }
        }
        if (disposableDomains.contains(lower)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "邮箱不支持，请更换邮箱");
        }
    }

    private String extractDomain(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int at = normalized.lastIndexOf('@');
        if (at <= 0 || at == normalized.length() - 1) {
            return null;
        }
        return normalized.substring(at + 1);
    }
}
