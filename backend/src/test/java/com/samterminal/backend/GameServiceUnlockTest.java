package com.samterminal.backend;

import com.samterminal.backend.dto.GameStateDto;
import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.GameState;
import com.samterminal.backend.entity.Location;
import com.samterminal.backend.entity.UserRole;
import com.samterminal.backend.repository.*;
import com.samterminal.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceUnlockTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private GameStateRepository stateRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private MemoryRepository memoryRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private LlmSettingRepository llmSettingRepository;
    @Mock
    private LlmApiConfigRepository llmApiConfigRepository;
    @Mock
    private UserLocationUnlockRepository unlockRepository;
    @Mock
    private TokenUsageService tokenUsageService;
    @Mock
    private LlmService llmService;
    @Mock
    private LlmPoolService llmPoolService;
    @Mock
    private SessionService sessionService;
    @Mock
    private MemoryRagService memoryRagService;
    @Mock
    private UserLocationUnlockService unlockService;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(
                userRepository,
                stateRepository,
                locationRepository,
                itemRepository,
                memoryRepository,
                chatMessageRepository,
                llmSettingRepository,
                llmApiConfigRepository,
                unlockRepository,
                tokenUsageService,
                llmService,
                llmPoolService,
                sessionService,
                memoryRagService,
                unlockService
        );
    }

    @Test
    void getStateTriggersUnlockServiceForDefaults() {
        AppUser user = AppUser.builder()
                .id(1L)
                .username("test-dup")
                .role(UserRole.USER)
                .build();
        Location location = Location.builder()
                .id(10L)
                .code("golden-hour")
                .name("黄金的时刻")
                .unlocked(true)
                .build();
        GameState state = GameState.builder()
                .id(100L)
                .user(user)
                .currentLocation(location)
                .build();

        when(userRepository.findByUsername("test-dup")).thenReturn(Optional.of(user));
        when(stateRepository.findByUser(user)).thenReturn(Optional.of(state));
        when(locationRepository.findAll()).thenReturn(List.of(location));
        when(itemRepository.findByUser(user)).thenReturn(List.of());
        when(memoryRepository.findByUser(user)).thenReturn(List.of());

        GameStateDto dto = gameService.getState("test-dup");

        assertThat(dto.getCurrentLocation()).isEqualTo("golden-hour");
        verify(unlockService).ensureUnlocked(user, location);
    }
}
