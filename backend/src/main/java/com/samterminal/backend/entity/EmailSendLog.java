package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_send_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 36)
    private String requestId;

    private String username;
    private String email;
    private String ip;

    @Column(name = "code_masked")
    private String codeMasked;

    @Column(name = "code_encrypted", length = 2048)
    private String codeEncrypted;

    @Enumerated(EnumType.STRING)
    private EmailSendLogStatus status;

    @Column(name = "smtp_id")
    private Long smtpId;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
