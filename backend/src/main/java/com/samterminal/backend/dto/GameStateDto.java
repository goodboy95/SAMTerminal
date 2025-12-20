package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateDto {
    private String currentLocation;
    private String currentLocationName;
    private String locationDynamicState;
    private String fireflyEmotion;
    private String fireflyStatus;
    private String fireflyMoodDetails;
    private String gameTime;
    private List<ItemDto> items;
    private List<MemoryDto> memories;
    private String userName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        private Long id;
        private String name;
        private String description;
        private String icon;
        private int quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryDto {
        private Long id;
        private String title;
        private String content;
        private String date;
        private List<String> tags;
    }
}
