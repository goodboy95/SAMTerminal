package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateUpdateDto {
    private LocationUpdate location;
    private FireflyUpdate firefly;
    private InventoryChange inventoryChange;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationUpdate {
        private String id;
        private String name;
        private String backgroundUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FireflyUpdate {
        private String emotion;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryChange {
        private Long itemId;
        private int delta;
    }
}
