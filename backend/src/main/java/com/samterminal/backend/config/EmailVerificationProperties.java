package com.samterminal.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "email")
public class EmailVerificationProperties {
    private Duration codeTtl = Duration.ofMinutes(5);
    private Duration resendInterval = Duration.ofMinutes(1);
    private int maxVerifyAttempts = 5;
    private int autoBanThreshold = 50;
    private Duration autoBanExtra = Duration.ofMinutes(5);
    private RateLimit rateLimit = new RateLimit();
    private Cap cap = new Cap();
    private DomainPolicy domainPolicy = new DomainPolicy();
    private Smtp smtp = new Smtp();
    private SendTask sendTask = new SendTask();
    private Encryption encryption = new Encryption();

    @Data
    public static class RateLimit {
        private int challengePerMinute = 30;
        private int verifyPerMinute = 30;
        private int sendPerMinute = 10;
        private int verifyCodePerMinute = 30;
    }

    @Data
    public static class Cap {
        private String baseUrl;
        private String siteKey;
        private String siteSecret;
        private boolean testMode = false;
        private int requestTimeoutSeconds = 5;
    }

    @Data
    public static class DomainPolicy {
        private List<String> allowlist = new ArrayList<>();
        private List<String> denylist = new ArrayList<>();
        private String disposableListPath = "classpath:disposable-email-domains.txt";
        private boolean enabled = true;
    }

    @Data
    public static class Smtp {
        private int failureThreshold = 3;
        private int circuitOpenMinutes = 10;
        private int connectTimeoutSeconds = 5;
        private int readTimeoutSeconds = 10;
        private int maxAttempts = 3;
    }

    @Data
    public static class SendTask {
        private int batchSize = 10;
        private int maxAttempts = 3;
        private int initialBackoffSeconds = 10;
        private int maxBackoffSeconds = 300;
        private int workerDelaySeconds = 2;
    }

    @Data
    public static class Encryption {
        private String keyBase64;
        private String codeHashSalt;
    }
}
