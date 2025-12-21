package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_send_log_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendLogAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_id")
    private Long logId;

    @Column(name = "admin_username")
    private String adminUsername;

    @Column(name = "admin_ip")
    private String adminIp;

    private String action;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
