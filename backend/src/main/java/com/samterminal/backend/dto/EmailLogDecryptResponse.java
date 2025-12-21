package com.samterminal.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailLogDecryptResponse {
    private String code;
}
