package com.samterminal.backend.repository;

import com.samterminal.backend.entity.EmailIpBan;
import com.samterminal.backend.entity.EmailIpBanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailIpBanRepository extends JpaRepository<EmailIpBan, Long> {
    Optional<EmailIpBan> findByIp(String ip);

    List<EmailIpBan> findByIpIn(List<String> ips);

    List<EmailIpBan> findByTypeAndBannedUntilBefore(EmailIpBanType type, LocalDateTime time);
}
