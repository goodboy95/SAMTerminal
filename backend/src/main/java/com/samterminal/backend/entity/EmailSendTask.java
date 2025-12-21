package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_send_task")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 36)
    private String requestId;

    private String username;
    private String email;
    private String ip;

    @Column(name = "code_encrypted", length = 2048)
    private String codeEncrypted;

    @Enumerated(EnumType.STRING)
    private EmailSendTaskStatus status;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 512)
    private String lastError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
