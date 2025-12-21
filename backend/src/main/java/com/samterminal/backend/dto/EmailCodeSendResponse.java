package com.samterminal.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailCodeSendResponse {
    private String requestId;
    private LocalDateTime resendAvailableAt;
    private LocalDateTime expiresAt;
    private String sendStatus;
}
