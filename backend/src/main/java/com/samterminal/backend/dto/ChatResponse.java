package com.samterminal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private List<ChatMessageDto> messages;
    private GameStateDto state;
    private StateUpdateDto stateUpdate;
    private String sessionId;
}
