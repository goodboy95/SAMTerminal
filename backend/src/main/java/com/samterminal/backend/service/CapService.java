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
public class CapService {
    private final EmailVerificationProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public CapService(EmailVerificationProperties properties,
                      ObjectMapper objectMapper,
                      RestTemplateBuilder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(properties.getCap().getRequestTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(properties.getCap().getRequestTimeoutSeconds()))
                .build();
    }

    public boolean verifyToken(String token, String ip) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (properties.getCap().isTestMode()) {
            return true;
        }
        String baseUrl = properties.getCap().getBaseUrl();
        String siteKey = properties.getCap().getSiteKey();
        String siteSecret = properties.getCap().getSiteSecret();
        if (baseUrl == null || baseUrl.isBlank() || siteKey == null || siteKey.isBlank()
                || siteSecret == null || siteSecret.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CAP 服务未配置");
        }
        String url = baseUrl + "/" + siteKey + "/siteverify";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("secret", siteSecret, "response", token), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return false;
        }
        try {
            Map<String, Object> data = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            Object success = data.get("success");
            return success instanceof Boolean && (Boolean) success;
        } catch (Exception ex) {
            return false;
        }
    }
}
