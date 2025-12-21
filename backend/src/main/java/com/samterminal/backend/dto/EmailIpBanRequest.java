package com.samterminal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailIpBanRequest {
    @NotBlank
    private String ip;

    @NotNull
    private LocalDateTime bannedUntil;

    private String reason;
}
