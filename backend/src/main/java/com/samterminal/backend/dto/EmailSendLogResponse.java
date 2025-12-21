package com.samterminal.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailSendLogResponse {
    private Long id;
    private String username;
    private String ip;
    private String email;
    private String codeMasked;
    private LocalDateTime sentAt;
    private Long smtpId;
    private String status;
}
