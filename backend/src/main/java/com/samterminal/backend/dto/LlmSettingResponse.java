package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmSettingResponse {
    private String baseUrl;
    private String modelName;
    private Double temperature;
}
