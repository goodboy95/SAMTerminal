package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private String id;
    private String name;
    private String description;
    private String backgroundStyle;
    private String backgroundUrl;
    private double x;
    private double y;
    private boolean unlocked;
    private String domainId;
}
