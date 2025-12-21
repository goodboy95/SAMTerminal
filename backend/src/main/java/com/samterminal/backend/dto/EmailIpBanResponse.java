package com.samterminal.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailIpBanResponse {
    private String ip;
    private String type;
    private LocalDateTime bannedUntil;
    private String reason;
}
