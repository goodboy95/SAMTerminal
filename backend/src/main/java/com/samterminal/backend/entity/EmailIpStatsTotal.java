package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_ip_stats_total")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailIpStatsTotal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String ip;

    @Column(name = "requested_count")
    private Integer requestedCount = 0;

    @Column(name = "unverified_count")
    private Integer unverifiedCount = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
