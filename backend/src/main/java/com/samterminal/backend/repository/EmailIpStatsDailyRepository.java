package com.samterminal.backend.repository;

import com.samterminal.backend.entity.EmailIpStatsDaily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmailIpStatsDailyRepository extends JpaRepository<EmailIpStatsDaily, Long> {
    Optional<EmailIpStatsDaily> findByIpAndDate(String ip, LocalDate date);

    Page<EmailIpStatsDaily> findByDate(LocalDate date, Pageable pageable);

    List<EmailIpStatsDaily> findByDateAndIpIn(LocalDate date, List<String> ips);
}
