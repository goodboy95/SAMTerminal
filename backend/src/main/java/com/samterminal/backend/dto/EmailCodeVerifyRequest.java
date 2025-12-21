package com.samterminal.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailCodeVerifyRequest {
    @NotBlank
    private String emailRequestId;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String emailCode;
}
