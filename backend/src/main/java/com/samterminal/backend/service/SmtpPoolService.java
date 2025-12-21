package com.samterminal.backend.service;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.entity.EmailSmtpConfig;
import com.samterminal.backend.exception.ApiException;
import com.samterminal.backend.repository.EmailSmtpConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class SmtpPoolService {
    private final EmailSmtpConfigRepository repository;
    private final EmailVerificationProperties properties;
    private final EmailSender emailSender;
    private final Clock clock;
    private final Random random = new Random();

    public SmtpPoolService(EmailSmtpConfigRepository repository,
                           EmailVerificationProperties properties,
                           EmailSender emailSender,
                           Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    public boolean hasAvailableSmtp() {
        LocalDateTime now = LocalDateTime.now(clock);
        return repository.findByEnabledTrue().stream().anyMatch(cfg -> isAvailable(cfg, now));
    }

    @Transactional
    public EmailSmtpConfig sendWithFailover(String to, String subject, String body) {
        List<EmailSmtpConfig> candidates = new ArrayList<>(repository.findByEnabledTrue());
        if (candidates.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "无可用 SMTP 服务");
        }
        Collections.shuffle(candidates, random);
        int maxAttempts = Math.min(properties.getSmtp().getMaxAttempts(), candidates.size());
        LocalDateTime now = LocalDateTime.now(clock);
        MailException lastError = null;
        for (int i = 0, tried = 0; i < candidates.size() && tried < maxAttempts; i++) {
            EmailSmtpConfig config = candidates.get(i);
            if (!isAvailable(config, now)) {
                continue;
            }
            tried++;
            try {
                emailSender.send(config, to, subject, body);
                recordSuccess(config, now);
                return config;
            } catch (MailException ex) {
                lastError = ex;
                recordFailure(config, now);
            }
        }
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "无可用 SMTP 服务" + (lastError != null ? ": " + lastError.getMessage() : ""));
    }

    @Transactional
    public void sendWithConfig(EmailSmtpConfig config, String to, String subject, String body) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!isAvailable(config, now)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP 当前不可用");
        }
        try {
            emailSender.send(config, to, subject, body);
            recordSuccess(config, now);
        } catch (MailException ex) {
            recordFailure(config, now);
            throw ex;
        }
    }

    private boolean isAvailable(EmailSmtpConfig config, LocalDateTime now) {
        if (config == null || !config.isEnabled()) {
            return false;
        }
        if (config.getCircuitOpenedAt() != null) {
            LocalDateTime reopenAt = config.getCircuitOpenedAt().plusMinutes(properties.getSmtp().getCircuitOpenMinutes());
            if (reopenAt.isAfter(now)) {
                return false;
            }
            config.setCircuitOpenedAt(null);
            config.setFailureCount(0);
            config.setUpdatedAt(now);
            repository.save(config);
        }
        ensureUsageWindow(config, now);
        if (config.getMaxPerMinute() != null && config.getSentMinuteCount() >= config.getMaxPerMinute()) {
            return false;
        }
        if (config.getMaxPerDay() != null && config.getSentDayCount() >= config.getMaxPerDay()) {
            return false;
        }
        return true;
    }

    private void ensureUsageWindow(EmailSmtpConfig config, LocalDateTime now) {
        LocalDateTime minuteWindow = config.getSentMinuteWindowStart();
        if (minuteWindow == null || minuteWindow.plusMinutes(1).isBefore(now)) {
            config.setSentMinuteWindowStart(now);
            config.setSentMinuteCount(0);
        }
        LocalDate day = config.getSentDayDate();
        if (day == null || day.isBefore(LocalDate.now(clock))) {
            config.setSentDayDate(LocalDate.now(clock));
            config.setSentDayCount(0);
        }
    }

    private void recordSuccess(EmailSmtpConfig config, LocalDateTime now) {
        ensureUsageWindow(config, now);
        config.setSentMinuteCount(config.getSentMinuteCount() + 1);
        config.setSentDayCount(config.getSentDayCount() + 1);
        config.setFailureCount(0);
        config.setCircuitOpenedAt(null);
        config.setLastSuccessAt(now);
        config.setUpdatedAt(now);
        repository.save(config);
    }

    private void recordFailure(EmailSmtpConfig config, LocalDateTime now) {
        int failureCount = config.getFailureCount() == null ? 0 : config.getFailureCount();
        failureCount += 1;
        config.setFailureCount(failureCount);
        config.setLastFailureAt(now);
        if (failureCount >= properties.getSmtp().getFailureThreshold()) {
            config.setCircuitOpenedAt(now);
        }
        config.setUpdatedAt(now);
        repository.save(config);
    }
}
