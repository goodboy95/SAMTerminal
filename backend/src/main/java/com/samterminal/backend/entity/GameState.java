package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class GameState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private AppUser user;

    @ManyToOne
    private Location currentLocation;

    @Column(length = 500)
    private String locationDynamicState;

    @Enumerated(EnumType.STRING)
    private Emotion fireflyEmotion;

    private String fireflyStatus;

    @Column(length = 500)
    private String fireflyMoodDetails;

    private String gameTime;
}
