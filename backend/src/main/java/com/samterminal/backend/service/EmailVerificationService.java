package com.samterminal.backend.service;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.dto.EmailCodeSendResponse;
import com.samterminal.backend.dto.EmailCodeVerifyResponse;
import com.samterminal.backend.entity.*;
import com.samterminal.backend.exception.ApiException;
import com.samterminal.backend.exception.ResendNotReadyException;
import com.samterminal.backend.repository.EmailSendLogRepository;
import com.samterminal.backend.repository.EmailSendTaskRepository;
import com.samterminal.backend.repository.EmailVerificationRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.security.SecureRandom;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private static final List<EmailVerificationRequestStatus> ACTIVE_STATUSES = List.of(
            EmailVerificationRequestStatus.PENDING,
            EmailVerificationRequestStatus.VERIFIED_PENDING_REGISTER
    );

    private final EmailVerificationRequestRepository requestRepository;
    private final EmailSendTaskRepository taskRepository;
    private final EmailSendLogRepository logRepository;
    private final EmailCryptoService cryptoService;
    private final EmailDomainPolicyService domainPolicyService;
    private final EmailIpStatsService ipStatsService;
    private final EmailIpBanService ipBanService;
    private final SmtpPoolService smtpPoolService;
    private final EmailVerificationProperties properties;
    private final AltchaService altchaService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(EmailVerificationRequestRepository requestRepository,
                                    EmailSendTaskRepository taskRepository,
                                    EmailSendLogRepository logRepository,
                                    EmailCryptoService cryptoService,
                                    EmailDomainPolicyService domainPolicyService,
                                    EmailIpStatsService ipStatsService,
                                    EmailIpBanService ipBanService,
                                    SmtpPoolService smtpPoolService,
                                    EmailVerificationProperties properties,
                                    AltchaService altchaService,
                                    Clock clock) {
        this.requestRepository = requestRepository;
        this.taskRepository = taskRepository;
        this.logRepository = logRepository;
        this.cryptoService = cryptoService;
        this.domainPolicyService = domainPolicyService;
        this.ipStatsService = ipStatsService;
        this.ipBanService = ipBanService;
        this.smtpPoolService = smtpPoolService;
        this.properties = properties;
        this.altchaService = altchaService;
        this.clock = clock;
    }

    @Transactional
    public EmailCodeSendResponse sendRegisterCode(String username, String email, String ip, String altchaPayload) {
        ipBanService.assertNotBanned(ip);
        domainPolicyService.validateEmail(email);
        if (!altchaService.verifyPayload(altchaPayload, ip)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALTCHA 校验失败");
        }
        if (!smtpPoolService.hasAvailableSmtp()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "无可用 SMTP 服务");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        requestRepository.expirePending(EmailVerificationRequestStatus.EXPIRED, EmailVerificationRequestStatus.PENDING, now);

        requestRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .ifPresent(latest -> {
                    if (latest.getResendAvailableAt() != null && latest.getResendAvailableAt().isAfter(now)) {
                        throw new ResendNotReadyException(latest.getResendAvailableAt());
                    }
                });

        String code = generateCode();
        String requestId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = now.plus(properties.getCodeTtl());
        LocalDateTime resendAvailableAt = now.plus(properties.getResendInterval());

        requestRepository.markByEmailWithStatus(email, EmailVerificationRequestStatus.SUPERSEDED, ACTIVE_STATUSES);

        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .id(requestId)
                .username(username)
                .email(email)
                .ip(ip)
                .codeHash(cryptoService.hashCode(code))
                .status(EmailVerificationRequestStatus.PENDING)
                .expiresAt(expiresAt)
                .resendAvailableAt(resendAvailableAt)
                .attemptCount(0)
                .createdAt(now)
                .build();
        requestRepository.save(request);

        String codeEncrypted = cryptoService.encrypt(code);
        EmailSendLog log = EmailSendLog.builder()
                .requestId(requestId)
                .username(username)
                .email(email)
                .ip(ip)
                .codeMasked(cryptoService.maskCode(code))
                .codeEncrypted(codeEncrypted)
                .status(EmailSendLogStatus.PENDING)
                .sentAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        logRepository.save(log);

        EmailSendTask task = EmailSendTask.builder()
                .requestId(requestId)
                .username(username)
                .email(email)
                .ip(ip)
                .codeEncrypted(codeEncrypted)
                .status(EmailSendTaskStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        taskRepository.save(task);

        var dailyStats = ipStatsService.incrementRequest(ip);
        ipBanService.banAutoIfNeeded(ip, dailyStats);

        return EmailCodeSendResponse.builder()
                .requestId(requestId)
                .expiresAt(expiresAt)
                .resendAvailableAt(resendAvailableAt)
                .sendStatus("PENDING")
                .build();
    }

    @Transactional
    public EmailCodeVerifyResponse verifyCode(String requestId, String email, String code, String ip) {
        EmailVerificationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "验证码请求不存在"));
        validateRequestMatch(request, email, ip);
        ensureNotExpired(request);
        if (request.getStatus() == EmailVerificationRequestStatus.VERIFIED_PENDING_REGISTER) {
            return EmailCodeVerifyResponse.builder()
                    .verified(true)
                    .expiresAt(request.getExpiresAt())
                    .attemptsRemaining(attemptsRemaining(request))
                    .build();
        }
        if (request.getStatus() != EmailVerificationRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码已失效");
        }
        ensureAttemptsRemaining(request);

        if (!request.getCodeHash().equals(cryptoService.hashCode(code))) {
            incrementAttempt(request);
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码错误");
        }
        request.setStatus(EmailVerificationRequestStatus.VERIFIED_PENDING_REGISTER);
        request.setVerifiedAt(LocalDateTime.now(clock));
        requestRepository.save(request);

        var dailyStats = ipStatsService.decrementUnverified(request.getIp());
        ipBanService.unbanAutoIfRecovered(request.getIp(), dailyStats);

        return EmailCodeVerifyResponse.builder()
                .verified(true)
                .expiresAt(request.getExpiresAt())
                .attemptsRemaining(attemptsRemaining(request))
                .build();
    }

    @Transactional
    public void consumeForRegister(String requestId, String email, String code, String ip, String username) {
        EmailVerificationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "验证码请求不存在"));
        if (username != null && request.getUsername() != null && !request.getUsername().equals(username)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码不匹配");
        }
        validateRequestMatch(request, email, ip);
        ensureNotExpired(request);

        if (request.getStatus() == EmailVerificationRequestStatus.USED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码已使用");
        }
        if (request.getStatus() == EmailVerificationRequestStatus.SUPERSEDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码已失效");
        }

        boolean decrementNeeded = false;
        if (request.getStatus() == EmailVerificationRequestStatus.VERIFIED_PENDING_REGISTER) {
            decrementNeeded = false;
        } else {
            ensureAttemptsRemaining(request);
            if (!request.getCodeHash().equals(cryptoService.hashCode(code))) {
                incrementAttempt(request);
                throw new ApiException(HttpStatus.BAD_REQUEST, "验证码错误");
            }
            decrementNeeded = true;
        }

        request.setStatus(EmailVerificationRequestStatus.USED);
        request.setUsedAt(LocalDateTime.now(clock));
        requestRepository.save(request);

        if (decrementNeeded) {
            var dailyStats = ipStatsService.decrementUnverified(request.getIp());
            ipBanService.unbanAutoIfRecovered(request.getIp(), dailyStats);
        }
    }

    public EmailSendTaskStatus getSendStatus(String requestId) {
        EmailSendTask task = taskRepository.findTopByRequestIdOrderByCreatedAtDesc(requestId);
        if (task == null) {
            return null;
        }
        return task.getStatus();
    }

    public String getSendLastError(String requestId) {
        EmailSendTask task = taskRepository.findTopByRequestIdOrderByCreatedAtDesc(requestId);
        return task != null ? task.getLastError() : null;
    }

    private void validateRequestMatch(EmailVerificationRequest request, String email, String ip) {
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码不匹配");
        }
        if (request.getIp() != null && ip != null && !request.getIp().equals(ip)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码不匹配");
        }
    }

    private void ensureNotExpired(EmailVerificationRequest request) {
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            request.setStatus(EmailVerificationRequestStatus.EXPIRED);
            requestRepository.save(request);
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码已过期");
        }
    }

    private void ensureAttemptsRemaining(EmailVerificationRequest request) {
        int attempts = request.getAttemptCount() == null ? 0 : request.getAttemptCount();
        if (attempts >= properties.getMaxVerifyAttempts()) {
            request.setStatus(EmailVerificationRequestStatus.EXPIRED);
            requestRepository.save(request);
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码已失效");
        }
    }

    private void incrementAttempt(EmailVerificationRequest request) {
        int attempts = request.getAttemptCount() == null ? 0 : request.getAttemptCount();
        attempts += 1;
        request.setAttemptCount(attempts);
        if (attempts >= properties.getMaxVerifyAttempts()) {
            request.setStatus(EmailVerificationRequestStatus.EXPIRED);
        }
        requestRepository.save(request);
    }

    private Integer attemptsRemaining(EmailVerificationRequest request) {
        int attempts = request.getAttemptCount() == null ? 0 : request.getAttemptCount();
        return Math.max(0, properties.getMaxVerifyAttempts() - attempts);
    }

    private String generateCode() {
        int code = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
