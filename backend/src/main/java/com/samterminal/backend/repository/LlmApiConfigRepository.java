package com.samterminal.backend.repository;

import com.samterminal.backend.entity.LlmApiConfig;
import com.samterminal.backend.entity.LlmApiStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LlmApiConfigRepository extends JpaRepository<LlmApiConfig, Long> {
    List<LlmApiConfig> findByStatus(LlmApiStatus status);
}
