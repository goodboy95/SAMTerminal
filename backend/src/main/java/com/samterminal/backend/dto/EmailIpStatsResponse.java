package com.samterminal.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailIpStatsResponse {
    private String ip;
    private int requestedToday;
    private int unverifiedToday;
    private int requestedTotal;
    private int unverifiedTotal;
    private String banStatus;
    private LocalDateTime bannedUntil;
}
