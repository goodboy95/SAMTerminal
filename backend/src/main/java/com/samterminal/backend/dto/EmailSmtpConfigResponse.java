package com.samterminal.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailSmtpConfigResponse {
    private Long id;
    private String name;
    private String host;
    private Integer port;
    private String username;
    private String fromAddress;
    private boolean useTls;
    private boolean useSsl;
    private boolean enabled;
    private Integer maxPerMinute;
    private Integer maxPerDay;
    private Integer failureCount;
    private LocalDateTime lastFailureAt;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime circuitOpenedAt;
    private boolean hasPassword;
    private String status;
}
