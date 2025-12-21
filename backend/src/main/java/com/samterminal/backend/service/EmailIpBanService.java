package com.samterminal.backend.service;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.entity.EmailIpBan;
import com.samterminal.backend.entity.EmailIpBanType;
import com.samterminal.backend.entity.EmailIpStatsDaily;
import com.samterminal.backend.exception.ApiException;
import com.samterminal.backend.repository.EmailIpBanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class EmailIpBanService {
    private final EmailIpBanRepository banRepository;
    private final EmailVerificationProperties properties;
    private final Clock clock;

    public EmailIpBanService(EmailIpBanRepository banRepository,
                             EmailVerificationProperties properties,
                             Clock clock) {
        this.banRepository = banRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public EmailIpBan getActiveBan(String ip) {
        return banRepository.findByIp(ip)
                .map(ban -> {
                    if (ban.getBannedUntil() == null || !ban.getBannedUntil().isAfter(LocalDateTime.now(clock))) {
                        banRepository.delete(ban);
                        return null;
                    }
                    return ban;
                })
                .orElse(null);
    }

    public void assertNotBanned(String ip) {
        EmailIpBan ban = getActiveBan(ip);
        if (ban != null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "IP 已被封禁");
        }
    }

    @Transactional
    public void banAutoIfNeeded(String ip, EmailIpStatsDaily dailyStats) {
        if (dailyStats == null) {
            return;
        }
        if (dailyStats.getUnverifiedCount() <= properties.getAutoBanThreshold()) {
            return;
        }
        EmailIpBan existing = banRepository.findByIp(ip).orElse(null);
        if (existing != null && existing.getType() == EmailIpBanType.MANUAL) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime bannedUntil = LocalDate.now(clock).plusDays(1).atStartOfDay().plus(properties.getAutoBanExtra());
        EmailIpBan ban = existing != null ? existing : new EmailIpBan();
        ban.setIp(ip);
        ban.setType(EmailIpBanType.AUTO);
        ban.setBannedUntil(bannedUntil);
        ban.setReason("AUTO: unverified over threshold");
        if (ban.getCreatedAt() == null) {
            ban.setCreatedAt(now);
        }
        ban.setUpdatedAt(now);
        banRepository.save(ban);
    }

    @Transactional
    public void unbanAutoIfRecovered(String ip, EmailIpStatsDaily dailyStats) {
        if (dailyStats == null || dailyStats.getUnverifiedCount() > properties.getAutoBanThreshold()) {
            return;
        }
        EmailIpBan ban = banRepository.findByIp(ip).orElse(null);
        if (ban != null && ban.getType() == EmailIpBanType.AUTO) {
            banRepository.delete(ban);
        }
    }

    @Transactional
    public EmailIpBan manualBan(String ip, LocalDateTime bannedUntil, String reason) {
        if (bannedUntil == null || bannedUntil.isBefore(LocalDateTime.now(clock))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "解封时间必须晚于当前时间");
        }
        EmailIpBan ban = banRepository.findByIp(ip).orElse(new EmailIpBan());
        ban.setIp(ip);
        ban.setType(EmailIpBanType.MANUAL);
        ban.setBannedUntil(bannedUntil);
        ban.setReason(reason);
        if (ban.getCreatedAt() == null) {
            ban.setCreatedAt(LocalDateTime.now(clock));
        }
        ban.setUpdatedAt(LocalDateTime.now(clock));
        return banRepository.save(ban);
    }

    @Transactional
    public void manualUnban(String ip) {
        EmailIpBan ban = banRepository.findByIp(ip).orElse(null);
        if (ban == null) {
            return;
        }
        if (ban.getType() != EmailIpBanType.MANUAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "仅支持解封手动封禁 IP");
        }
        banRepository.delete(ban);
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void clearExpiredAutoBans() {
        LocalDateTime now = LocalDateTime.now(clock);
        banRepository.findByTypeAndBannedUntilBefore(EmailIpBanType.AUTO, now)
                .forEach(banRepository::delete);
    }
}
