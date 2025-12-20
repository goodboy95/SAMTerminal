package com.samterminal.backend.service;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.SystemSetting;
import com.samterminal.backend.entity.UserTokenLimit;
import com.samterminal.backend.entity.UserTokenUsage;
import com.samterminal.backend.repository.SystemSettingRepository;
import com.samterminal.backend.repository.UserTokenLimitRepository;
import com.samterminal.backend.repository.UserTokenUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TokenUsageService {
    public static final String GLOBAL_LIMIT_KEY = "global_token_limit";
    private static final long DEFAULT_GLOBAL_LIMIT = 50000L;

    private final UserTokenUsageRepository usageRepository;
    private final UserTokenLimitRepository limitRepository;
    private final SystemSettingRepository settingRepository;

    public TokenUsageService(UserTokenUsageRepository usageRepository,
                             UserTokenLimitRepository limitRepository,
                             SystemSettingRepository settingRepository) {
        this.usageRepository = usageRepository;
        this.limitRepository = limitRepository;
        this.settingRepository = settingRepository;
    }

    public long getGlobalLimit() {
        return settingRepository.findBySettingKey(GLOBAL_LIMIT_KEY)
                .map(setting -> parseLong(setting.getSettingValue(), DEFAULT_GLOBAL_LIMIT))
                .orElse(DEFAULT_GLOBAL_LIMIT);
    }

    @Transactional
    public void setGlobalLimit(long value) {
        SystemSetting setting = settingRepository.findBySettingKey(GLOBAL_LIMIT_KEY)
                .orElseGet(() -> SystemSetting.builder().settingKey(GLOBAL_LIMIT_KEY).build());
        setting.setSettingValue(String.valueOf(value));
        settingRepository.save(setting);
    }

    public Long getCustomLimit(AppUser user) {
        return limitRepository.findByUser(user).map(UserTokenLimit::getCustomLimit).orElse(null);
    }

    public long resolveLimit(AppUser user) {
        Long custom = getCustomLimit(user);
        return custom != null ? custom : getGlobalLimit();
    }

    @Transactional
    public UserTokenUsage getOrCreateUsage(AppUser user) {
        return usageRepository.findByUser(user)
                .orElseGet(() -> usageRepository.save(UserTokenUsage.builder()
                        .user(user)
                        .inputTokens(0L)
                        .outputTokens(0L)
                        .updatedAt(Instant.now())
                        .build()));
    }

    public long weightedTokens(long input, long output) {
        return input + output * 8L;
    }

    public long currentWeightedUsage(AppUser user) {
        UserTokenUsage usage = getOrCreateUsage(user);
        return weightedTokens(usage.getInputTokens(), usage.getOutputTokens());
    }

    public boolean wouldExceedLimit(AppUser user, long additionalInput, long additionalOutput) {
        long limit = resolveLimit(user);
        long current = currentWeightedUsage(user);
        long next = current + weightedTokens(additionalInput, additionalOutput);
        return next > limit;
    }

    @Transactional
    public void recordUsage(AppUser user, long inputTokens, long outputTokens) {
        UserTokenUsage usage = getOrCreateUsage(user);
        usage.setInputTokens(usage.getInputTokens() + inputTokens);
        usage.setOutputTokens(usage.getOutputTokens() + outputTokens);
        usage.setUpdatedAt(Instant.now());
        usageRepository.save(usage);
    }

    @Transactional
    public void setUserLimit(AppUser user, Long limit) {
        UserTokenLimit tokenLimit = limitRepository.findByUser(user)
                .orElseGet(() -> UserTokenLimit.builder().user(user).build());
        tokenLimit.setCustomLimit(limit);
        limitRepository.save(tokenLimit);
    }

    private long parseLong(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
