package com.samterminal.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmailSmtpConfigRequest {
    private String name;

    @NotBlank
    private String host;

    @NotNull
    private Integer port;

    private String username;

    private String password;

    @NotBlank
    @Email
    private String fromAddress;

    private Boolean useTls;
    private Boolean useSsl;
    private Boolean enabled;
    private Integer maxPerMinute;
    private Integer maxPerDay;
}
