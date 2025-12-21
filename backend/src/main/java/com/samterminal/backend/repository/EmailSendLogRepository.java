package com.samterminal.backend.repository;

import com.samterminal.backend.entity.EmailSendLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface EmailSendLogRepository extends JpaRepository<EmailSendLog, Long> {
    Page<EmailSendLog> findBySentAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    EmailSendLog findTopByRequestIdOrderByCreatedAtDesc(String requestId);
}
