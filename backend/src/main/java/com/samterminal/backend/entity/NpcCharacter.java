package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class NpcCharacter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(length = 1000)
    private String prompt;
    private String role;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String avatarUrl;
}
