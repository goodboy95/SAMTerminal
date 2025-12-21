package com.samterminal.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailSmtpTestRequest {
    @NotBlank
    @Email
    private String toEmail;
}
