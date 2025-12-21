package com.samterminal.backend;

import com.samterminal.backend.entity.*;
import com.samterminal.backend.repository.ChatSessionRepository;
import com.samterminal.backend.repository.LlmApiConfigRepository;
import com.samterminal.backend.repository.AppUserRepository;
import com.samterminal.backend.repository.ChatMessageRepository;
import com.samterminal.backend.repository.GameStateRepository;
import com.samterminal.backend.repository.ItemRepository;
import com.samterminal.backend.repository.MemoryRepository;
import com.samterminal.backend.repository.UserTokenLimitRepository;
import com.samterminal.backend.repository.UserTokenUsageRepository;
import com.samterminal.backend.repository.UserLocationUnlockRepository;
import com.samterminal.backend.service.SessionService;
import com.samterminal.backend.service.LlmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SessionServiceTest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private LlmApiConfigRepository apiRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private UserLocationUnlockRepository unlockRepository;

    @Autowired
    private GameStateRepository stateRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserTokenLimitRepository tokenLimitRepository;

    @Autowired
    private UserTokenUsageRepository tokenUsageRepository;

    @MockBean
    private LlmService llmService;

    @BeforeEach
    void setup() {
        chatMessageRepository.deleteAll();
        sessionRepository.deleteAll();
        unlockRepository.deleteAll();
        stateRepository.deleteAll();
        itemRepository.deleteAll();
        memoryRepository.deleteAll();
        tokenLimitRepository.deleteAll();
        tokenUsageRepository.deleteAll();
        apiRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsAndExpiresSession() {
        LlmApiConfig api = apiRepository.save(LlmApiConfig.builder()
                .name("Primary")
                .baseUrl("https://api.example.com")
                .modelName("model")
                .role(LlmApiRole.PRIMARY)
                .status(LlmApiStatus.ACTIVE)
                .maxLoad(10)
                .tokenLimit(10000L)
                .tokenUsed(0L)
                .build());

        AppUser user = userRepository.save(AppUser.builder()
                .username("session-user")
                .role(UserRole.USER)
                .build());

        ChatSession session = sessionService.createSession(user);
        assertThat(session.getActiveApi()).isNotNull();
        assertThat(session.getActiveApi().getId()).isEqualTo(api.getId());

        session.setLastActiveAt(Instant.now().minus(40, ChronoUnit.MINUTES));
        sessionRepository.save(session);

        ChatSession refreshed = sessionService.resolveSession(user, session.getSessionId());
        assertThat(refreshed.getSessionId()).isNotEqualTo(session.getSessionId());
        ChatSession old = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(old.getStatus()).isEqualTo(ChatSessionStatus.EXPIRED);
    }
}
