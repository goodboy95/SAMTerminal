package com.samterminal.backend.service;

import com.samterminal.backend.dto.LlmApiConfigRequest;
import com.samterminal.backend.dto.LlmApiConfigResponse;
import com.samterminal.backend.entity.LlmApiConfig;
import com.samterminal.backend.entity.LlmApiRole;
import com.samterminal.backend.entity.LlmApiStatus;
import com.samterminal.backend.repository.LlmApiConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

@Service
public class LlmApiConfigService {
    private final LlmApiConfigRepository apiRepository;
    private final ApiLoadTracker loadTracker;
    private final LlmService llmService;

    public LlmApiConfigService(LlmApiConfigRepository apiRepository,
                               ApiLoadTracker loadTracker,
                               LlmService llmService) {
        this.apiRepository = apiRepository;
        this.loadTracker = loadTracker;
        this.llmService = llmService;
    }

    @Transactional(readOnly = true)
    public List<LlmApiConfigResponse> listConfigs() {
        return apiRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LlmApiConfigResponse createConfig(LlmApiConfigRequest request) {
        validate(request);
        LlmApiConfig config = new LlmApiConfig();
        applyRequest(config, request, true);
        return toResponse(apiRepository.save(config));
    }

    @Transactional
    public LlmApiConfigResponse updateConfig(Long id, LlmApiConfigRequest request) {
        LlmApiConfig config = apiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API config not found"));
        validate(request);
        applyRequest(config, request, false);
        return toResponse(apiRepository.save(config));
    }

    @Transactional
    public void deleteConfig(Long id) {
        apiRepository.deleteById(id);
    }

    @Transactional
    public LlmApiConfigResponse resetTokens(Long id) {
        LlmApiConfig config = apiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API config not found"));
        config.setTokenUsed(0L);
        config.setFailureCount(0);
        return toResponse(apiRepository.save(config));
    }

    @Transactional(readOnly = true)
    public boolean testConfig(Long id) {
        LlmApiConfig config = apiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API config not found"));
        return llmService.testConnection(config);
    }

    private void applyRequest(LlmApiConfig config, LlmApiConfigRequest request, boolean create) {
        config.setName(request.getName());
        config.setBaseUrl(request.getBaseUrl());
        if (request.getApiKey() != null) {
            config.setApiKey(StringUtils.hasText(request.getApiKey()) ? request.getApiKey() : null);
        }
        config.setModelName(request.getModelName());
        config.setTemperature(request.getTemperature());
        if (request.getRole() != null) {
            config.setRole(request.getRole());
        } else if (create && config.getRole() == null) {
            config.setRole(LlmApiRole.PRIMARY);
        }
        if (request.getTokenLimit() != null) {
            config.setTokenLimit(request.getTokenLimit());
        }
        if (request.getMaxLoad() != null) {
            config.setMaxLoad(request.getMaxLoad());
        }
        if (request.getStatus() != null) {
            config.setStatus(request.getStatus());
        } else if (create && config.getStatus() == null) {
            config.setStatus(LlmApiStatus.ACTIVE);
        }
        if (config.getTokenUsed() == null) {
            config.setTokenUsed(0L);
        }
        if (config.getFailureCount() == null) {
            config.setFailureCount(0);
        }
    }

    private LlmApiConfigResponse toResponse(LlmApiConfig config) {
        return new LlmApiConfigResponse(
                config.getId(),
                config.getName(),
                config.getBaseUrl(),
                maskApiKey(config.getApiKey()),
                config.getModelName(),
                config.getTemperature(),
                config.getRole(),
                config.getTokenLimit(),
                config.getTokenUsed(),
                config.getStatus(),
                config.getFailureCount(),
                config.getLastFailureAt(),
                config.getLastSuccessAt(),
                config.getCircuitOpenedAt(),
                config.getMaxLoad(),
                loadTracker.currentLoad(config.getId()),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 6) {
            return "***";
        }
        return trimmed.substring(0, 3) + "***" + trimmed.substring(trimmed.length() - 2);
    }

    private void validate(LlmApiConfigRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (!StringUtils.hasText(request.getBaseUrl())) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (!StringUtils.hasText(request.getModelName())) {
            throw new IllegalArgumentException("modelName is required");
        }
        if (request.getName() != null && request.getName().length() > 100) {
            throw new IllegalArgumentException("name too long");
        }
        if (request.getBaseUrl().length() > 255) {
            throw new IllegalArgumentException("baseUrl too long");
        }
        if (request.getModelName().length() > 100) {
            throw new IllegalArgumentException("modelName too long");
        }
        if (request.getTokenLimit() != null && request.getTokenLimit() < 0) {
            throw new IllegalArgumentException("tokenLimit must be >= 0");
        }
        if (request.getMaxLoad() != null && request.getMaxLoad() <= 0) {
            throw new IllegalArgumentException("maxLoad must be > 0");
        }
        if (request.getTemperature() != null && (request.getTemperature() < 0 || request.getTemperature() > 2)) {
            throw new IllegalArgumentException("temperature must be between 0 and 2");
        }
        validateBaseUrl(request.getBaseUrl());
    }

    private void validateBaseUrl(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("baseUrl must use http/https");
            }
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                throw new IllegalArgumentException("baseUrl must include host");
            }
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()) {
                throw new IllegalArgumentException("baseUrl is not allowed");
            }
            if ("localhost".equalsIgnoreCase(host)) {
                throw new IllegalArgumentException("baseUrl is not allowed");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("baseUrl is invalid");
        }
    }
}
