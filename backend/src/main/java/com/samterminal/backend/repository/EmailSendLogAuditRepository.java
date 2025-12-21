package com.samterminal.backend.repository;

import com.samterminal.backend.entity.EmailSendLogAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSendLogAuditRepository extends JpaRepository<EmailSendLogAudit, Long> {
}
