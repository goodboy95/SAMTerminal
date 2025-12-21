package com.samterminal.backend.service;

import com.samterminal.backend.entity.EmailIpStatsDaily;
import com.samterminal.backend.entity.EmailIpStatsTotal;
import com.samterminal.backend.repository.EmailIpStatsDailyRepository;
import com.samterminal.backend.repository.EmailIpStatsTotalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class EmailIpStatsService {
    private final EmailIpStatsDailyRepository dailyRepository;
    private final EmailIpStatsTotalRepository totalRepository;
    private final Clock clock;

    public EmailIpStatsService(EmailIpStatsDailyRepository dailyRepository,
                               EmailIpStatsTotalRepository totalRepository,
                               Clock clock) {
        this.dailyRepository = dailyRepository;
        this.totalRepository = totalRepository;
        this.clock = clock;
    }

    @Transactional
    public EmailIpStatsDaily incrementRequest(String ip) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        EmailIpStatsDaily daily = dailyRepository.findByIpAndDate(ip, today)
                .orElseGet(() -> EmailIpStatsDaily.builder()
                        .ip(ip)
                        .date(today)
                        .requestedCount(0)
                        .unverifiedCount(0)
                        .build());
        daily.setRequestedCount(daily.getRequestedCount() + 1);
        daily.setUnverifiedCount(daily.getUnverifiedCount() + 1);
        daily.setUpdatedAt(now);
        dailyRepository.save(daily);

        EmailIpStatsTotal total = totalRepository.findByIp(ip)
                .orElseGet(() -> EmailIpStatsTotal.builder()
                        .ip(ip)
                        .requestedCount(0)
                        .unverifiedCount(0)
                        .build());
        total.setRequestedCount(total.getRequestedCount() + 1);
        total.setUnverifiedCount(total.getUnverifiedCount() + 1);
        total.setUpdatedAt(now);
        totalRepository.save(total);

        return daily;
    }

    @Transactional
    public EmailIpStatsDaily decrementUnverified(String ip) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        EmailIpStatsDaily daily = dailyRepository.findByIpAndDate(ip, today)
                .orElseGet(() -> EmailIpStatsDaily.builder()
                        .ip(ip)
                        .date(today)
                        .requestedCount(0)
                        .unverifiedCount(0)
                        .build());
        daily.setUnverifiedCount(Math.max(0, daily.getUnverifiedCount() - 1));
        daily.setUpdatedAt(now);
        dailyRepository.save(daily);

        EmailIpStatsTotal total = totalRepository.findByIp(ip)
                .orElseGet(() -> EmailIpStatsTotal.builder()
                        .ip(ip)
                        .requestedCount(0)
                        .unverifiedCount(0)
                        .build());
        total.setUnverifiedCount(Math.max(0, total.getUnverifiedCount() - 1));
        total.setUpdatedAt(now);
        totalRepository.save(total);

        return daily;
    }
}
