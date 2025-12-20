package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FireflyAssetRequest {
    private Map<String, String> assets; // emotion -> url
}
