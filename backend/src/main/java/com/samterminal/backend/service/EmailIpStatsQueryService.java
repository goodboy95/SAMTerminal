package com.samterminal.backend.service;

import com.samterminal.backend.dto.EmailIpStatsResponse;
import com.samterminal.backend.entity.EmailIpBan;
import com.samterminal.backend.entity.EmailIpStatsDaily;
import com.samterminal.backend.entity.EmailIpStatsTotal;
import com.samterminal.backend.repository.EmailIpBanRepository;
import com.samterminal.backend.repository.EmailIpStatsDailyRepository;
import com.samterminal.backend.repository.EmailIpStatsTotalRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmailIpStatsQueryService {
    private final EmailIpStatsDailyRepository dailyRepository;
    private final EmailIpStatsTotalRepository totalRepository;
    private final EmailIpBanRepository banRepository;

    public EmailIpStatsQueryService(EmailIpStatsDailyRepository dailyRepository,
                                    EmailIpStatsTotalRepository totalRepository,
                                    EmailIpBanRepository banRepository) {
        this.dailyRepository = dailyRepository;
        this.totalRepository = totalRepository;
        this.banRepository = banRepository;
    }

    public Page<EmailIpStatsResponse> query(LocalDate date, int page, int size, String sortField, Sort.Direction direction) {
        PageRequest pageRequest;
        if (isTotalSort(sortField)) {
            pageRequest = PageRequest.of(page, size, Sort.by(direction, mapTotalSortField(sortField)));
            Page<EmailIpStatsTotal> totals = totalRepository.findAll(pageRequest);
            List<String> ips = totals.getContent().stream().map(EmailIpStatsTotal::getIp).toList();
            Map<String, EmailIpStatsDaily> dailyMap = dailyRepository.findByDateAndIpIn(date, ips).stream()
                    .collect(Collectors.toMap(EmailIpStatsDaily::getIp, d -> d));
            Map<String, EmailIpBan> banMap = toBanMap(ips);
            List<EmailIpStatsResponse> items = totals.getContent().stream()
                    .map(total -> toResponse(total, dailyMap.get(total.getIp()), banMap.get(total.getIp())))
                    .toList();
            return new PageImpl<>(items, pageRequest, totals.getTotalElements());
        }
        pageRequest = PageRequest.of(page, size, Sort.by(direction, mapDailySortField(sortField)));
        Page<EmailIpStatsDaily> dailyPage = dailyRepository.findByDate(date, pageRequest);
        List<String> ips = dailyPage.getContent().stream().map(EmailIpStatsDaily::getIp).toList();
        Map<String, EmailIpStatsTotal> totalMap = totalRepository.findByIpIn(ips).stream()
                .collect(Collectors.toMap(EmailIpStatsTotal::getIp, t -> t));
        Map<String, EmailIpBan> banMap = toBanMap(ips);
        List<EmailIpStatsResponse> items = dailyPage.getContent().stream()
                .map(daily -> toResponse(totalMap.get(daily.getIp()), daily, banMap.get(daily.getIp())))
                .toList();
        return new PageImpl<>(items, pageRequest, dailyPage.getTotalElements());
    }

    private Map<String, EmailIpBan> toBanMap(List<String> ips) {
        if (ips.isEmpty()) {
            return new HashMap<>();
        }
        return banRepository.findByIpIn(ips).stream()
                .collect(Collectors.toMap(EmailIpBan::getIp, ban -> ban));
    }

    private EmailIpStatsResponse toResponse(EmailIpStatsTotal total, EmailIpStatsDaily daily, EmailIpBan ban) {
        int requestedToday = daily != null ? daily.getRequestedCount() : 0;
        int unverifiedToday = daily != null ? daily.getUnverifiedCount() : 0;
        int requestedTotal = total != null ? total.getRequestedCount() : 0;
        int unverifiedTotal = total != null ? total.getUnverifiedCount() : 0;
        String banStatus = ban == null ? "NONE" : ban.getType().name();
        return EmailIpStatsResponse.builder()
                .ip(daily != null ? daily.getIp() : total != null ? total.getIp() : null)
                .requestedToday(requestedToday)
                .unverifiedToday(unverifiedToday)
                .requestedTotal(requestedTotal)
                .unverifiedTotal(unverifiedTotal)
                .banStatus(banStatus)
                .bannedUntil(ban != null ? ban.getBannedUntil() : null)
                .build();
    }

    private boolean isTotalSort(String sortField) {
        return "requestedTotal".equalsIgnoreCase(sortField) || "unverifiedTotal".equalsIgnoreCase(sortField);
    }

    private String mapDailySortField(String sortField) {
        if ("requestedToday".equalsIgnoreCase(sortField)) {
            return "requestedCount";
        }
        if ("unverifiedToday".equalsIgnoreCase(sortField)) {
            return "unverifiedCount";
        }
        return "ip";
    }

    private String mapTotalSortField(String sortField) {
        if ("requestedTotal".equalsIgnoreCase(sortField)) {
            return "requestedCount";
        }
        if ("unverifiedTotal".equalsIgnoreCase(sortField)) {
            return "unverifiedCount";
        }
        return "ip";
    }
}
