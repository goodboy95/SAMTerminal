package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_ip_stats_daily", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ip", "stats_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailIpStatsDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ip;

    @Column(name = "stats_date")
    private LocalDate date;

    @Column(name = "requested_count")
    private Integer requestedCount = 0;

    @Column(name = "unverified_count")
    private Integer unverifiedCount = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
