package com.samterminal.backend.service;

import com.samterminal.backend.dto.EmailLogDecryptResponse;
import com.samterminal.backend.dto.EmailSendLogResponse;
import com.samterminal.backend.entity.EmailSendLog;
import com.samterminal.backend.entity.EmailSendLogAudit;
import com.samterminal.backend.exception.ApiException;
import com.samterminal.backend.repository.EmailSendLogAuditRepository;
import com.samterminal.backend.repository.EmailSendLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class EmailSendLogService {
    private final EmailSendLogRepository logRepository;
    private final EmailSendLogAuditRepository auditRepository;
    private final EmailCryptoService cryptoService;
    private final Clock clock;

    public EmailSendLogService(EmailSendLogRepository logRepository,
                               EmailSendLogAuditRepository auditRepository,
                               EmailCryptoService cryptoService,
                               Clock clock) {
        this.logRepository = logRepository;
        this.auditRepository = auditRepository;
        this.cryptoService = cryptoService;
        this.clock = clock;
    }

    public EmailSendLogResponse toResponse(EmailSendLog log) {
        return EmailSendLogResponse.builder()
                .id(log.getId())
                .username(log.getUsername())
                .ip(log.getIp())
                .email(log.getEmail())
                .codeMasked(log.getCodeMasked())
                .sentAt(log.getSentAt())
                .smtpId(log.getSmtpId())
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .build();
    }

    @Transactional
    public EmailLogDecryptResponse decryptCode(Long logId, String adminIp) {
        EmailSendLog log = logRepository.findById(logId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "日志不存在"));
        if (log.getCodeEncrypted() == null || log.getCodeEncrypted().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码不可解密");
        }
        String code = cryptoService.decrypt(log.getCodeEncrypted());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";
        EmailSendLogAudit audit = EmailSendLogAudit.builder()
                .logId(logId)
                .adminUsername(username)
                .adminIp(adminIp)
                .action("DECRYPT_CODE")
                .createdAt(LocalDateTime.now(clock))
                .build();
        auditRepository.save(audit);
        return EmailLogDecryptResponse.builder().code(code).build();
    }
}
