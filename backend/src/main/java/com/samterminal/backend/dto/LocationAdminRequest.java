package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationAdminRequest {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String aiDescription;
    private String backgroundStyle;
    private String backgroundUrl;
    private double coordX;
    private double coordY;
    private boolean unlocked;
    private String domainCode;
}
