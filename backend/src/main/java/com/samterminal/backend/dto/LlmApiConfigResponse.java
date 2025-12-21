package com.samterminal.backend.dto;

import com.samterminal.backend.entity.LlmApiRole;
import com.samterminal.backend.entity.LlmApiStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmApiConfigResponse {
    private Long id;
    private String name;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Double temperature;
    private LlmApiRole role;
    private Long tokenLimit;
    private Long tokenUsed;
    private LlmApiStatus status;
    private Integer failureCount;
    private Instant lastFailureAt;
    private Instant lastSuccessAt;
    private Instant circuitOpenedAt;
    private Integer maxLoad;
    private Integer currentLoad;
    private Instant createdAt;
    private Instant updatedAt;
}
