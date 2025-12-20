package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StarDomainDto {
    private String id;
    private String name;
    private String description;
    private double x;
    private double y;
    private String color;
}
