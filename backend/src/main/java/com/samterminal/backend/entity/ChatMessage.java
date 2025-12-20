package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender; // user, firefly, npc, system
    private String npcName;
    @Column(length = 1000)
    private String content;
    private String narration;
    private Instant timestamp;

    @ManyToOne
    private AppUser user;
}
