package com.samterminal.backend.dto;

import com.samterminal.backend.entity.LlmApiRole;
import com.samterminal.backend.entity.LlmApiStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmApiConfigRequest {
    private String name;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Double temperature;
    private LlmApiRole role;
    private Long tokenLimit;
    private Integer maxLoad;
    private LlmApiStatus status;
}
