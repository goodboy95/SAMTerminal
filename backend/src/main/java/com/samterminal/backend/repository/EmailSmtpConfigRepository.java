package com.samterminal.backend.repository;

import com.samterminal.backend.entity.EmailSmtpConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailSmtpConfigRepository extends JpaRepository<EmailSmtpConfig, Long> {
    List<EmailSmtpConfig> findByEnabledTrue();
}
