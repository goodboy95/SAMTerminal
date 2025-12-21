package com.samterminal.backend.service;

import com.samterminal.backend.entity.LlmApiConfig;
import com.samterminal.backend.entity.LlmApiRole;
import com.samterminal.backend.entity.LlmApiStatus;
import com.samterminal.backend.entity.LlmSetting;
import com.samterminal.backend.repository.LlmApiConfigRepository;
import com.samterminal.backend.repository.LlmSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LlmSettingMigrationService {
    private final LlmSettingRepository llmSettingRepository;
    private final LlmApiConfigRepository apiConfigRepository;

    public LlmSettingMigrationService(LlmSettingRepository llmSettingRepository,
                                      LlmApiConfigRepository apiConfigRepository) {
        this.llmSettingRepository = llmSettingRepository;
        this.apiConfigRepository = apiConfigRepository;
    }

    @Transactional
    public void migrateIfNeeded() {
        if (apiConfigRepository.count() > 0) {
            return;
        }
        LlmSetting setting = llmSettingRepository.findAll().stream().findFirst().orElse(null);
        if (setting == null || setting.getBaseUrl() == null || setting.getModelName() == null) {
            return;
        }
        LlmApiConfig config = LlmApiConfig.builder()
                .name("Migrated LLM")
                .baseUrl(setting.getBaseUrl())
                .apiKey(setting.getApiKey())
                .modelName(setting.getModelName())
                .temperature(setting.getTemperature())
                .role(LlmApiRole.PRIMARY)
                .status(LlmApiStatus.ACTIVE)
                .tokenUsed(0L)
                .failureCount(0)
                .maxLoad(30)
                .build();
        apiConfigRepository.save(config);
    }
}
