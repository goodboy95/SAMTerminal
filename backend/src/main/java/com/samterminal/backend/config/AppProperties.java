package com.samterminal.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private List<AdminAccount> admins = new ArrayList<>();
    private LlmSettings llm = new LlmSettings();

    @Data
    public static class AdminAccount {
        private String username;
        private String password;
        private String email;
    }

    @Data
    public static class LlmSettings {
        private long minRemainingTokens = 2000L;
        private int minRemainingPercent = 5;
        private int sessionTimeoutMinutes = 30;
        private int requestTimeoutSeconds = 20;
        private CircuitBreaker circuitBreaker = new CircuitBreaker();
    }

    @Data
    public static class CircuitBreaker {
        private int failureThreshold = 3;
        private int probeIntervalMinutes = 10;
    }
}
