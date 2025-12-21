package com.samterminal.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class RequestIpResolver {
    private final Set<String> trustedProxies;

    public RequestIpResolver(@Value("${app.trusted-proxies:127.0.0.1,::1}") String trustedProxyList) {
        this.trustedProxies = new HashSet<>();
        Arrays.stream(trustedProxyList.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(trustedProxies::add);
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && trustedProxies.contains(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
