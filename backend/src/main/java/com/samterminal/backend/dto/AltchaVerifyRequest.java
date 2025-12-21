package com.samterminal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AltchaVerifyRequest {
    @NotBlank
    private String payload;
}
