package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequest {
    @Id
    @Column(length = 36)
    private String id;

    private String username;
    private String email;
    private String ip;

    @Column(name = "code_hash", length = 128)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    private EmailVerificationRequestStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "resend_available_at")
    private LocalDateTime resendAvailableAt;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
