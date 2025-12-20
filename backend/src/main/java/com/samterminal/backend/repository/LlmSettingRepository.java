package com.samterminal.backend.repository;

import com.samterminal.backend.entity.LlmSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmSettingRepository extends JpaRepository<LlmSetting, Long> {
}
