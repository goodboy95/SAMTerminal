package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacterAdminRequest {
    private Long id;
    private String name;
    private String role;
    private String prompt;
    private String description;
    private String avatarUrl;
}
