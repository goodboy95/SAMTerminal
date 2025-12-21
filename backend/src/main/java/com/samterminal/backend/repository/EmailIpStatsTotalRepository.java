package com.samterminal.backend.repository;

import com.samterminal.backend.entity.EmailIpStatsTotal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailIpStatsTotalRepository extends JpaRepository<EmailIpStatsTotal, Long> {
    Optional<EmailIpStatsTotal> findByIp(String ip);

    List<EmailIpStatsTotal> findByIpIn(List<String> ips);

    Page<EmailIpStatsTotal> findAll(Pageable pageable);
}
