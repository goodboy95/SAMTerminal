package com.samterminal.backend.service;

import com.samterminal.backend.config.AppProperties;
import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.ChatSession;
import com.samterminal.backend.entity.ChatSessionStatus;
import com.samterminal.backend.entity.LlmApiConfig;
import com.samterminal.backend.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {
    private final ChatSessionRepository sessionRepository;
    private final LlmPoolService llmPoolService;
    private final AppProperties appProperties;

    public SessionService(ChatSessionRepository sessionRepository,
                          LlmPoolService llmPoolService,
                          AppProperties appProperties) {
        this.sessionRepository = sessionRepository;
        this.llmPoolService = llmPoolService;
        this.appProperties = appProperties;
    }

    @Transactional
    public ChatSession resolveSession(AppUser user, String sessionId) {
        ChatSession session = null;
        if (sessionId != null && !sessionId.isBlank()) {
            Optional<ChatSession> existing = sessionRepository.findBySessionId(sessionId);
            if (existing.isPresent() && existing.get().getUser() != null && existing.get().getUser().getId().equals(user.getId())) {
                session = existing.get();
            }
        }
        if (session == null || isExpired(session)) {
            if (session != null) {
                session.setStatus(ChatSessionStatus.EXPIRED);
                sessionRepository.save(session);
            }
            return createSession(user);
        }
        session.setLastActiveAt(Instant.now());
        return sessionRepository.save(session);
    }

    @Transactional
    public ChatSession createSession(AppUser user) {
        ChatSession session = ChatSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .user(user)
                .status(ChatSessionStatus.ACTIVE)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .build();
        LlmApiConfig api = llmPoolService.selectApiForNewSession();
        session.setActiveApi(api);
        return sessionRepository.save(session);
    }

    private boolean isExpired(ChatSession session) {
        if (session == null || session.getLastActiveAt() == null) {
            return true;
        }
        long timeoutMinutes = appProperties.getLlm().getSessionTimeoutMinutes();
        Instant cutoff = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);
        return session.getLastActiveAt().isBefore(cutoff);
    }
}
