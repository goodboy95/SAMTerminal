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
import com.samterminal.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class LlmPoolServiceTest {

    @Autowired
    private LlmPoolService llmPoolService;

    @Autowired
    private LlmApiConfigRepository apiRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ApiLoadTracker loadTracker;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private SessionService sessionService;

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
    void selectsPrimaryByCapacity() {
        LlmApiConfig apiA = apiRepository.save(LlmApiConfig.builder()
                .name("A")
                .baseUrl("https://api.example.com")
                .modelName("model-a")
                .role(LlmApiRole.PRIMARY)
                .status(LlmApiStatus.ACTIVE)
                .maxLoad(5)
                .tokenLimit(10000L)
                .tokenUsed(0L)
                .build());
        LlmApiConfig apiB = apiRepository.save(LlmApiConfig.builder()
                .name("B")
                .baseUrl("https://api.example.com")
                .modelName("model-b")
                .role(LlmApiRole.PRIMARY)
                .status(LlmApiStatus.ACTIVE)
                .maxLoad(5)
                .tokenLimit(10000L)
                .tokenUsed(0L)
                .build());

        loadTracker.recordCall(apiA.getId());

        LlmApiConfig selected = llmPoolService.selectApiForNewSession();
        assertThat(selected).isNotNull();
        assertThat(selected.getId()).isEqualTo(apiB.getId());
    }

    @Test
    void opensCircuitAfterFailures() {
        LlmApiConfig api = apiRepository.save(LlmApiConfig.builder()
                .name("Fail")
                .baseUrl("https://api.example.com")
                .modelName("model-fail")
                .role(LlmApiRole.PRIMARY)
                .status(LlmApiStatus.ACTIVE)
                .maxLoad(5)
                .tokenLimit(10000L)
                .tokenUsed(0L)
                .build());

        AppUser user = userRepository.save(AppUser.builder()
                .username("pool-user")
                .role(UserRole.USER)
                .build());
        ChatSession session = sessionService.createSession(user);
        session.setActiveApi(api);
        sessionRepository.save(session);

        when(llmService.callLlm(any(LlmApiConfig.class), anyString(), anyString())).thenReturn(null);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> llmPoolService.callWithSession(session, "sys", "user"))
                    .isInstanceOf(NoAvailableApiException.class);
        }

        LlmApiConfig updated = apiRepository.findById(api.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LlmApiStatus.CIRCUIT_OPEN);
        assertThat(updated.getFailureCount()).isGreaterThanOrEqualTo(3);
    }
}
