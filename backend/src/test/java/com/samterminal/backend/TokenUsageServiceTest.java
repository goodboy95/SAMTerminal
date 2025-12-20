package com.samterminal.backend;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.UserRole;
import com.samterminal.backend.repository.AppUserRepository;
import com.samterminal.backend.service.TokenUsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TokenUsageServiceTest {

    @Autowired
    private TokenUsageService tokenUsageService;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void tracksUsageAndLimits() {
        AppUser user = userRepository.save(AppUser.builder()
                .username("usage-user")
                .role(UserRole.USER)
                .build());

        tokenUsageService.setGlobalLimit(100L);
        assertThat(tokenUsageService.resolveLimit(user)).isEqualTo(100L);

        tokenUsageService.setUserLimit(user, 50L);
        assertThat(tokenUsageService.resolveLimit(user)).isEqualTo(50L);

        tokenUsageService.recordUsage(user, 10L, 2L); // weighted 26
        assertThat(tokenUsageService.currentWeightedUsage(user)).isEqualTo(26L);
        assertThat(tokenUsageService.wouldExceedLimit(user, 10L, 0L)).isFalse();
        assertThat(tokenUsageService.wouldExceedLimit(user, 30L, 0L)).isTrue();
    }
}
