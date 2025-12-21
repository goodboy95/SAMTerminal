package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_ip_ban", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ip"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailIpBan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ip;

    @Enumerated(EnumType.STRING)
    private EmailIpBanType type;

    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;

    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
