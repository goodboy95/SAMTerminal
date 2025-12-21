package com.samterminal.backend.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class LlmClientConfig {

    @Bean
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder, AppProperties appProperties) {
        int timeoutSeconds = appProperties.getLlm().getRequestTimeoutSeconds();
        return builder
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
