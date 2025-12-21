package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "llm_api_config")
public class LlmApiConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    private Double temperature;

    @Enumerated(EnumType.STRING)
    private LlmApiRole role;

    @Column(name = "token_limit")
    private Long tokenLimit;

    @Column(name = "token_used")
    private Long tokenUsed;

    @Enumerated(EnumType.STRING)
    private LlmApiStatus status;

    @Column(name = "failure_count")
    private Integer failureCount;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "circuit_opened_at")
    private Instant circuitOpenedAt;

    @Column(name = "max_load")
    private Integer maxLoad;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (role == null) {
            role = LlmApiRole.PRIMARY;
        }
        if (status == null) {
            status = LlmApiStatus.ACTIVE;
        }
        if (tokenUsed == null) {
            tokenUsed = 0L;
        }
        if (failureCount == null) {
            failureCount = 0;
        }
        if (maxLoad == null) {
            maxLoad = 30;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (tokenUsed == null) {
            tokenUsed = 0L;
        }
        if (failureCount == null) {
            failureCount = 0;
        }
    }
}
