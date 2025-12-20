package com.samterminal.backend;

import com.samterminal.backend.dto.GameStateDto;
import com.samterminal.backend.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Test
    void defaultStateReturnsLocation() {
        GameStateDto dto = gameService.getState("test-user");
        assertThat(dto.getCurrentLocation()).isNotNull();
        assertThat(dto.getItems()).isNotEmpty();
    }
}
