package com.samterminal.backend.service;

import com.samterminal.backend.config.AppProperties;
import com.samterminal.backend.entity.*;
import com.samterminal.backend.repository.ChatSessionRepository;
import com.samterminal.backend.repository.LlmApiConfigRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class LlmPoolService {
    private final LlmApiConfigRepository apiRepository;
    private final ChatSessionRepository sessionRepository;
    private final LlmService llmService;
    private final ApiLoadTracker loadTracker;
    private final AppProperties appProperties;

    public LlmPoolService(LlmApiConfigRepository apiRepository,
                          ChatSessionRepository sessionRepository,
                          LlmService llmService,
                          ApiLoadTracker loadTracker,
                          AppProperties appProperties) {
        this.apiRepository = apiRepository;
        this.sessionRepository = sessionRepository;
        this.llmService = llmService;
        this.loadTracker = loadTracker;
        this.appProperties = appProperties;
    }

    public record LlmCallResult(LlmService.LlmReply reply, LlmApiConfig apiConfig) {}

    public LlmApiConfig selectApiForNewSession() {
        return selectApi(null);
    }

    public LlmApiConfig resolveSessionApi(ChatSession session) {
        if (session == null) {
            return selectApiForNewSession();
        }
        LlmApiConfig bound = session.getActiveApi();
        if (bound != null && isApiAvailable(bound)) {
            return bound;
        }
        return selectApi(bound != null ? bound.getId() : null);
    }

    public LlmCallResult callWithSession(ChatSession session, String systemPrompt, String userPrompt) {
        LlmApiConfig primary = resolveSessionApi(session);
        if (primary == null) {
            throw new NoAvailableApiException("暂无可用模型，请稍后再试。");
        }
        LlmCallResult result = attemptCall(primary, systemPrompt, userPrompt);
        if (result != null) {
            updateSession(session, primary);
            return result;
        }
        markFailure(primary);
        LlmApiConfig fallback = selectApi(primary.getId());
        if (fallback == null) {
            throw new NoAvailableApiException("暂无可用模型，请稍后再试。");
        }
        LlmCallResult retry = attemptCall(fallback, systemPrompt, userPrompt);
        if (retry != null) {
            updateSession(session, fallback);
            return retry;
        }
        markFailure(fallback);
        throw new NoAvailableApiException("暂无可用模型，请稍后再试。");
    }

    @Transactional
    public void markFailure(LlmApiConfig config) {
        if (config == null) {
            return;
        }
        LlmApiConfig managed = apiRepository.findById(config.getId()).orElse(null);
        if (managed == null) {
            return;
        }
        int failures = managed.getFailureCount() != null ? managed.getFailureCount() + 1 : 1;
        managed.setFailureCount(failures);
        managed.setLastFailureAt(Instant.now());
        if (failures >= appProperties.getLlm().getCircuitBreaker().getFailureThreshold()) {
            managed.setStatus(LlmApiStatus.CIRCUIT_OPEN);
            managed.setCircuitOpenedAt(Instant.now());
        }
        apiRepository.save(managed);
    }

    @Transactional
    public void markSuccess(LlmApiConfig config, long tokensUsed) {
        if (config == null) {
            return;
        }
        LlmApiConfig managed = apiRepository.findById(config.getId()).orElse(null);
        if (managed == null) {
            return;
        }
        long current = managed.getTokenUsed() != null ? managed.getTokenUsed() : 0L;
        managed.setTokenUsed(current + tokensUsed);
        managed.setFailureCount(0);
        managed.setLastSuccessAt(Instant.now());
        if (managed.getStatus() == LlmApiStatus.CIRCUIT_OPEN) {
            managed.setStatus(LlmApiStatus.ACTIVE);
            managed.setCircuitOpenedAt(null);
        }
        apiRepository.save(managed);
    }

    @Transactional
    public void updateSession(ChatSession session, LlmApiConfig config) {
        if (session == null) {
            return;
        }
        ChatSession managed = sessionRepository.findById(session.getId()).orElse(null);
        if (managed == null) {
            return;
        }
        if (config != null && (managed.getActiveApi() == null || !Objects.equals(managed.getActiveApi().getId(), config.getId()))) {
            managed.setActiveApi(config);
        }
        managed.setLastActiveAt(Instant.now());
        sessionRepository.save(managed);
    }

    public int currentLoad(Long apiId) {
        return loadTracker.currentLoad(apiId);
    }

    @Scheduled(fixedDelayString = "#{${app.llm.circuit-breaker.probe-interval-minutes:10} * 60000}")
    public void probeCircuitOpenApis() {
        List<LlmApiConfig> openApis = apiRepository.findByStatus(LlmApiStatus.CIRCUIT_OPEN);
        if (openApis.isEmpty()) {
            return;
        }
        int intervalMinutes = appProperties.getLlm().getCircuitBreaker().getProbeIntervalMinutes();
        Instant cutoff = Instant.now().minus(intervalMinutes, ChronoUnit.MINUTES);
        for (LlmApiConfig api : openApis) {
            if (api.getCircuitOpenedAt() != null && api.getCircuitOpenedAt().isAfter(cutoff)) {
                continue;
            }
            boolean ok = llmService.testConnection(api);
            if (ok) {
                resetCircuit(api);
            }
        }
    }

    @Transactional
    public void resetCircuit(LlmApiConfig config) {
        if (config == null) {
            return;
        }
        LlmApiConfig managed = apiRepository.findById(config.getId()).orElse(null);
        if (managed == null) {
            return;
        }
        managed.setStatus(LlmApiStatus.ACTIVE);
        managed.setFailureCount(0);
        managed.setCircuitOpenedAt(null);
        managed.setLastSuccessAt(Instant.now());
        apiRepository.save(managed);
    }

    private LlmCallResult attemptCall(LlmApiConfig api, String systemPrompt, String userPrompt) {
        loadTracker.recordCall(api.getId());
        LlmService.LlmReply reply = llmService.callLlm(api, systemPrompt, userPrompt);
        if (reply == null) {
            return null;
        }
        long inputTokens = reply.inputTokens() > 0 ? reply.inputTokens() : TokenEstimator.estimateTokens(systemPrompt) + TokenEstimator.estimateTokens(userPrompt);
        long outputTokens = reply.outputTokens() > 0 ? reply.outputTokens() : TokenEstimator.estimateTokens(reply.content());
        markSuccess(api, inputTokens + outputTokens);
        return new LlmCallResult(reply, api);
    }

    private LlmApiConfig selectApi(Long excludeId) {
        List<LlmApiConfig> all = apiRepository.findAll();
        if (all.isEmpty()) {
            return null;
        }
        List<LlmApiConfig> primaries = all.stream()
                .filter(api -> api.getRole() == null || api.getRole() == LlmApiRole.PRIMARY)
                .toList();
        LlmApiConfig selected = selectFromPool(primaries, excludeId);
        if (selected != null) {
            return selected;
        }
        List<LlmApiConfig> backups = all.stream()
                .filter(api -> api.getRole() == LlmApiRole.BACKUP)
                .toList();
        return selectFromPool(backups, excludeId);
    }

    private LlmApiConfig selectFromPool(List<LlmApiConfig> pool, Long excludeId) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        List<LlmApiConfig> available = pool.stream()
                .filter(this::isApiAvailable)
                .filter(api -> excludeId == null || !Objects.equals(api.getId(), excludeId))
                .toList();
        if (available.isEmpty()) {
            return null;
        }
        List<LlmApiConfig> sufficient = available.stream()
                .filter(this::isTokenSufficient)
                .toList();
        if (!sufficient.isEmpty()) {
            return pickBest(sufficient);
        }
        return pickBest(available);
    }

    private LlmApiConfig pickBest(List<LlmApiConfig> candidates) {
        return candidates.stream()
                .max(Comparator.comparingInt(this::availableCapacity)
                        .thenComparingLong(this::remainingTokens))
                .orElse(null);
    }

    private boolean isApiAvailable(LlmApiConfig api) {
        if (api == null) {
            return false;
        }
        if (api.getStatus() == LlmApiStatus.CIRCUIT_OPEN || api.getStatus() == LlmApiStatus.DISABLED) {
            return false;
        }
        if (isTokenExhausted(api)) {
            return false;
        }
        Integer maxLoad = api.getMaxLoad();
        if (maxLoad != null && maxLoad > 0) {
            return loadTracker.currentLoad(api.getId()) < maxLoad;
        }
        return maxLoad == null || maxLoad == 0 ? true : false;
    }

    private boolean isTokenExhausted(LlmApiConfig api) {
        if (api.getTokenLimit() == null) {
            return false;
        }
        long used = api.getTokenUsed() != null ? api.getTokenUsed() : 0L;
        return used >= api.getTokenLimit();
    }

    private boolean isTokenSufficient(LlmApiConfig api) {
        if (api.getTokenLimit() == null) {
            return true;
        }
        long remaining = remainingTokens(api);
        long minTokens = appProperties.getLlm().getMinRemainingTokens();
        long percentThreshold = Math.round(api.getTokenLimit() * (appProperties.getLlm().getMinRemainingPercent() / 100.0));
        long threshold = Math.max(minTokens, percentThreshold);
        return remaining >= threshold;
    }

    private long remainingTokens(LlmApiConfig api) {
        if (api.getTokenLimit() == null) {
            return Long.MAX_VALUE;
        }
        long used = api.getTokenUsed() != null ? api.getTokenUsed() : 0L;
        return api.getTokenLimit() - used;
    }

    private int availableCapacity(LlmApiConfig api) {
        Integer maxLoad = api.getMaxLoad();
        if (maxLoad == null) {
            return Integer.MAX_VALUE;
        }
        return maxLoad - loadTracker.currentLoad(api.getId());
    }
}
