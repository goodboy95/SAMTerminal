package com.samterminal.backend.repository;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByUserOrderByTimestampAsc(AppUser user);
}
