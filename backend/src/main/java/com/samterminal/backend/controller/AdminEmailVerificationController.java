package com.samterminal.backend.controller;

import com.samterminal.backend.dto.*;
import com.samterminal.backend.entity.EmailSendLog;
import com.samterminal.backend.exception.ApiException;
import com.samterminal.backend.repository.EmailSendLogRepository;
import com.samterminal.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/email-verification")
public class AdminEmailVerificationController {
    private final EmailSmtpConfigService smtpConfigService;
    private final SmtpPoolService smtpPoolService;
    private final EmailSendLogRepository logRepository;
    private final EmailSendLogService logService;
    private final EmailIpStatsQueryService ipStatsQueryService;
    private final EmailIpBanService ipBanService;
    private final RequestIpResolver ipResolver;

    public AdminEmailVerificationController(EmailSmtpConfigService smtpConfigService,
                                            SmtpPoolService smtpPoolService,
                                            EmailSendLogRepository logRepository,
                                            EmailSendLogService logService,
                                            EmailIpStatsQueryService ipStatsQueryService,
                                            EmailIpBanService ipBanService,
                                            RequestIpResolver ipResolver) {
        this.smtpConfigService = smtpConfigService;
        this.smtpPoolService = smtpPoolService;
        this.logRepository = logRepository;
        this.logService = logService;
        this.ipStatsQueryService = ipStatsQueryService;
        this.ipBanService = ipBanService;
        this.ipResolver = ipResolver;
    }

    @GetMapping("/smtp")
    public ResponseEntity<?> listSmtp() {
        return ResponseEntity.ok(smtpConfigService.list());
    }

    @PostMapping("/smtp")
    public ResponseEntity<?> createSmtp(@Valid @RequestBody EmailSmtpConfigRequest request) {
        return ResponseEntity.ok(smtpConfigService.create(request));
    }

    @PutMapping("/smtp/{id}")
    public ResponseEntity<?> updateSmtp(@PathVariable Long id, @Valid @RequestBody EmailSmtpConfigRequest request) {
        return ResponseEntity.ok(smtpConfigService.update(id, request));
    }

    @DeleteMapping("/smtp/{id}")
    public ResponseEntity<?> deleteSmtp(@PathVariable Long id) {
        smtpConfigService.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @PostMapping("/smtp/{id}/test")
    public ResponseEntity<?> testSmtp(@PathVariable Long id, @Valid @RequestBody EmailSmtpTestRequest request) {
        var config = smtpConfigService.getById(id);
        smtpPoolService.sendWithConfig(config, request.getToEmail(), "SMTP 测试邮件", "这是一封 SMTP 测试邮件。");
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @GetMapping("/logs")
    public ResponseEntity<?> logs(@RequestParam("start") String start,
                                  @RequestParam("end") String end,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "size", defaultValue = "20") int size,
                                  @RequestParam(value = "sort", defaultValue = "sentAt,desc") String sort) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        Page<EmailSendLog> logPage = logRepository.findBySentAtBetween(startTime, endTime, pageRequest);
        return ResponseEntity.ok(Map.of(
                "items", logPage.getContent().stream().map(logService::toResponse).toList(),
                "total", logPage.getTotalElements(),
                "page", logPage.getNumber(),
                "size", logPage.getSize()
        ));
    }

    @PostMapping("/logs/{id}/decrypt")
    public ResponseEntity<?> decrypt(@PathVariable Long id, HttpServletRequest request) {
        String ip = ipResolver.resolve(request);
        return ResponseEntity.ok(logService.decryptCode(id, ip));
    }

    @GetMapping("/ip-stats")
    public ResponseEntity<?> ipStats(@RequestParam(value = "date", required = false) String date,
                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                     @RequestParam(value = "size", defaultValue = "20") int size,
                                     @RequestParam(value = "sortField", defaultValue = "unverifiedToday") String sortField,
                                     @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageResult = ipStatsQueryService.query(targetDate, page, size, sortField, direction);
        return ResponseEntity.ok(Map.of(
                "items", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", pageResult.getNumber(),
                "size", pageResult.getSize()
        ));
    }

    @PostMapping("/ip-bans")
    public ResponseEntity<?> manualBan(@Valid @RequestBody EmailIpBanRequest request) {
        var ban = ipBanService.manualBan(request.getIp(), request.getBannedUntil(), request.getReason());
        return ResponseEntity.ok(EmailIpBanResponse.builder()
                .ip(ban.getIp())
                .type(ban.getType().name())
                .bannedUntil(ban.getBannedUntil())
                .reason(ban.getReason())
                .build());
    }

    @DeleteMapping("/ip-bans/{ip}")
    public ResponseEntity<?> manualUnban(@PathVariable String ip) {
        ipBanService.manualUnban(ip);
        return ResponseEntity.ok(Map.of("status", "unbanned"));
    }
}
