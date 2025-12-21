package com.samterminal.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samterminal.backend.entity.LlmApiConfig;
import com.samterminal.backend.entity.LlmSetting;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public LlmService(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public LlmReply callLlm(LlmSetting setting, String systemPrompt, String userPrompt) {
        if (setting == null) {
            return null;
        }
        LlmApiConfig config = LlmApiConfig.builder()
                .baseUrl(setting.getBaseUrl())
                .apiKey(setting.getApiKey())
                .modelName(setting.getModelName())
                .temperature(setting.getTemperature())
                .build();
        return callLlm(config, systemPrompt, userPrompt);
    }

    public LlmReply callLlm(LlmApiConfig config, String systemPrompt, String userPrompt) {
        if (config == null || config.getBaseUrl() == null || config.getModelName() == null) {
            return null;
        }
        try {
            String url = buildCompletionUrl(config.getBaseUrl());
            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModelName());
            body.put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            HttpHeaders headers = buildHeaders(config.getApiKey());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, entity, String.class);
            if (response == null) {
                return null;
            }
            return parseReply(response);
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean testConnection(LlmApiConfig config) {
        if (config == null || config.getBaseUrl() == null || config.getModelName() == null) {
            return false;
        }
        try {
            String url = buildCompletionUrl(config.getBaseUrl());
            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModelName());
            body.put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", "You are a health check agent. Reply with JSON."),
                    Map.of("role", "user", "content", "hi")
            ));
            HttpHeaders headers = buildHeaders(config.getApiKey());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, entity, String.class);
            return response != null && !response.isBlank();
        } catch (Exception ex) {
            return false;
        }
    }

    private LlmReply parseReply(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode choice = root.path("choices").isArray() && root.path("choices").size() > 0
                ? root.path("choices").get(0)
                : null;
        String content = choice != null ? choice.path("message").path("content").asText() : null;
        long inputTokens = root.path("usage").path("prompt_tokens").asLong(0);
        long outputTokens = root.path("usage").path("completion_tokens").asLong(0);
        if (content == null || content.isBlank()) {
            return null;
        }
        String normalized = extractJson(content);
        JsonNode json = objectMapper.readTree(normalized);
        return new LlmReply(
                json.path("content").asText(),
                json.path("emotion").asText("normal"),
                json.path("narration").asText(null),
                json.path("intent").asText("chat"),
                json.path("target_id").asText(null),
                inputTokens,
                outputTokens
        );
    }

    private String extractJson(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstBrace = trimmed.indexOf('{');
            int lastBrace = trimmed.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return trimmed.substring(firstBrace, lastBrace + 1);
            }
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return "{}";
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String buildCompletionUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized.endsWith("/v1")) {
            return normalized + "/chat/completions";
        }
        return normalized + "/v1/chat/completions";
    }

    private HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("Authorization", "Bearer " + apiKey);
        }
        return headers;
    }

    public record LlmReply(String content, String emotion, String narration, String intent, String targetId,
                           long inputTokens, long outputTokens) {}
}
