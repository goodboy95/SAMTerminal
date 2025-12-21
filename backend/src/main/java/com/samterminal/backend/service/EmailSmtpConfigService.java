package com.samterminal.backend.service;

import com.samterminal.backend.dto.EmailSmtpConfigRequest;
import com.samterminal.backend.dto.EmailSmtpConfigResponse;
import com.samterminal.backend.entity.EmailSmtpConfig;
import com.samterminal.backend.exception.ApiException;
import com.samterminal.backend.repository.EmailSmtpConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailSmtpConfigService {
    private final EmailSmtpConfigRepository repository;
    private final EmailCryptoService cryptoService;
    private final Clock clock;

    public EmailSmtpConfigService(EmailSmtpConfigRepository repository,
                                  EmailCryptoService cryptoService,
                                  Clock clock) {
        this.repository = repository;
        this.cryptoService = cryptoService;
        this.clock = clock;
    }

    public List<EmailSmtpConfigResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmailSmtpConfigResponse create(EmailSmtpConfigRequest request) {
        EmailSmtpConfig config = new EmailSmtpConfig();
        applyRequest(config, request, true);
        config.setCreatedAt(LocalDateTime.now(clock));
        config.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(config);
        return toResponse(config);
    }

    @Transactional
    public EmailSmtpConfigResponse update(Long id, EmailSmtpConfigRequest request) {
        EmailSmtpConfig config = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SMTP 配置不存在"));
        applyRequest(config, request, false);
        config.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(config);
        return toResponse(config);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public EmailSmtpConfig getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SMTP 配置不存在"));
    }

    private void applyRequest(EmailSmtpConfig config, EmailSmtpConfigRequest request, boolean allowEmptyPassword) {
        config.setName(request.getName());
        config.setHost(request.getHost());
        config.setPort(request.getPort());
        config.setUsername(request.getUsername());
        if (request.getPassword() != null) {
            if (!request.getPassword().isBlank()) {
                config.setPasswordEncrypted(cryptoService.encrypt(request.getPassword()));
            } else if (allowEmptyPassword) {
                config.setPasswordEncrypted(null);
            }
        }
        config.setFromAddress(request.getFromAddress());
        if (request.getUseTls() != null) {
            config.setUseTls(request.getUseTls());
        }
        if (request.getUseSsl() != null) {
            config.setUseSsl(request.getUseSsl());
        }
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        config.setMaxPerMinute(request.getMaxPerMinute());
        config.setMaxPerDay(request.getMaxPerDay());
    }

    private EmailSmtpConfigResponse toResponse(EmailSmtpConfig config) {
        String status;
        if (!config.isEnabled()) {
            status = "DISABLED";
        } else if (config.getCircuitOpenedAt() != null) {
            status = "CIRCUIT_OPEN";
        } else {
            status = "ACTIVE";
        }
        return EmailSmtpConfigResponse.builder()
                .id(config.getId())
                .name(config.getName())
                .host(config.getHost())
                .port(config.getPort())
                .username(config.getUsername())
                .fromAddress(config.getFromAddress())
                .useTls(config.isUseTls())
                .useSsl(config.isUseSsl())
                .enabled(config.isEnabled())
                .maxPerMinute(config.getMaxPerMinute())
                .maxPerDay(config.getMaxPerDay())
                .failureCount(config.getFailureCount())
                .lastFailureAt(config.getLastFailureAt())
                .lastSuccessAt(config.getLastSuccessAt())
                .circuitOpenedAt(config.getCircuitOpenedAt())
                .hasPassword(config.getPasswordEncrypted() != null && !config.getPasswordEncrypted().isBlank())
                .status(status)
                .build();
    }
}
