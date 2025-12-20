package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUsageDto {
    private Long id;
    private String username;
    private long inputTokens;
    private long outputTokens;
    private Long customLimit;
}
