package com.samterminal.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailCodeVerifyResponse {
    private boolean verified;
    private LocalDateTime expiresAt;
    private Integer attemptsRemaining;
}
