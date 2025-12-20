package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String id;
    private String sender;
    private String npcName;
    private String content;
    private String narration;
    private String timestamp;
}
