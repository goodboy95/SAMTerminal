package com.samterminal.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String name;
    @Column(length = 500)
    private String description;
    private String backgroundStyle;
    private String backgroundUrl;
    @Column(columnDefinition = "TEXT")
    private String aiDescription;
    private double coordX;
    private double coordY;
    private boolean unlocked;

    @ManyToOne
    private StarDomain domain;
}
