package com.samterminal.backend.service;

import com.samterminal.backend.entity.EmailIpBan;
import com.samterminal.backend.entity.EmailIpBanType;
import com.samterminal.backend.repository.EmailIpBanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmailIpBanServiceTest {

    @Autowired
    private EmailIpStatsService ipStatsService;

    @Autowired
    private EmailIpBanService ipBanService;

    @Autowired
    private EmailIpBanRepository banRepository;

    @BeforeEach
    void setup() {
        banRepository.deleteAll();
    }

    @Test
    void autoBanAndUnban() {
        String ip = "9.9.9.9";
        var stats = ipStatsService.incrementRequest(ip);
        stats.setUnverifiedCount(51);
        ipBanService.banAutoIfNeeded(ip, stats);
        EmailIpBan ban = banRepository.findByIp(ip).orElse(null);
        assertThat(ban).isNotNull();
        assertThat(ban.getType()).isEqualTo(EmailIpBanType.AUTO);

        stats.setUnverifiedCount(50);
        ipBanService.unbanAutoIfRecovered(ip, stats);
        assertThat(banRepository.findByIp(ip)).isEmpty();
    }

    @Test
    void manualBanNotAutoRemoved() {
        String ip = "8.8.8.8";
        ipBanService.manualBan(ip, LocalDateTime.now().plusHours(2), "manual");
        EmailIpBan ban = banRepository.findByIp(ip).orElse(null);
        assertThat(ban).isNotNull();
        assertThat(ban.getType()).isEqualTo(EmailIpBanType.MANUAL);

        var stats = ipStatsService.incrementRequest(ip);
        stats.setUnverifiedCount(10);
        ipBanService.unbanAutoIfRecovered(ip, stats);
        assertThat(banRepository.findByIp(ip)).isPresent();
    }
}
