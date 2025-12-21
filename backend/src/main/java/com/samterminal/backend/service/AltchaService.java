package com.samterminal.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.exception.ApiException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class AltchaService {
    private final EmailVerificationProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public AltchaService(EmailVerificationProperties properties,
                         ObjectMapper objectMapper,
                         RestTemplateBuilder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(properties.getAltcha().getRequestTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(properties.getAltcha().getRequestTimeoutSeconds()))
                .build();
    }

    public Map<String, Object> fetchChallenge(String ip) {
        if (properties.getAltcha().isTestMode()) {
            return Map.of("testMode", true);
        }
        String baseUrl = properties.getAltcha().getSentinelBaseUrl();
        String apiKey = properties.getAltcha().getApiKey();
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ALTCHA 服务未配置");
        }
        String url = baseUrl + "/v1/challenge?apiKey=" + apiKey;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ALTCHA challenge 获取失败");
        }
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ALTCHA challenge 解析失败");
        }
    }

    public boolean verifyPayload(String payload, String ip) {
        if (payload == null || payload.isBlank()) {
            return false;
        }
        if (properties.getAltcha().isTestMode()) {
            return true;
        }
        String baseUrl = properties.getAltcha().getSentinelBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ALTCHA 服务未配置");
        }
        String url = baseUrl + "/v1/verify/signature";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("payload", payload), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return false;
        }
        try {
            Map<String, Object> data = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            Object verified = data.get("verified");
            return verified instanceof Boolean && (Boolean) verified;
        } catch (Exception ex) {
            return false;
        }
    }
}
