package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_smtp_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSmtpConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String host;
    private Integer port;
    private String username;

    @Column(name = "password_encrypted", length = 2048)
    private String passwordEncrypted;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "use_tls")
    private boolean useTls = true;

    @Column(name = "use_ssl")
    private boolean useSsl = false;

    private boolean enabled = true;

    @Column(name = "max_per_minute")
    private Integer maxPerMinute;

    @Column(name = "max_per_day")
    private Integer maxPerDay;

    @Column(name = "failure_count")
    private Integer failureCount = 0;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "circuit_opened_at")
    private LocalDateTime circuitOpenedAt;

    @Column(name = "sent_minute_count")
    private Integer sentMinuteCount = 0;

    @Column(name = "sent_minute_window_start")
    private LocalDateTime sentMinuteWindowStart;

    @Column(name = "sent_day_count")
    private Integer sentDayCount = 0;

    @Column(name = "sent_day_date")
    private LocalDate sentDayDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
