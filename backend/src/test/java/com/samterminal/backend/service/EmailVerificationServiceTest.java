package com.samterminal.backend.service;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.entity.EmailSendLog;
import com.samterminal.backend.entity.EmailSendTask;
import com.samterminal.backend.entity.EmailVerificationRequest;
import com.samterminal.backend.entity.EmailVerificationRequestStatus;
import com.samterminal.backend.exception.ResendNotReadyException;
import com.samterminal.backend.repository.EmailSendLogRepository;
import com.samterminal.backend.repository.EmailSendTaskRepository;
import com.samterminal.backend.repository.EmailVerificationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
class EmailVerificationServiceTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailVerificationRequestRepository requestRepository;

    @Autowired
    private EmailSendTaskRepository taskRepository;

    @Autowired
    private EmailSendLogRepository logRepository;

    @Autowired
    private EmailCryptoService cryptoService;

    @Autowired
    private EmailVerificationProperties properties;

    @MockBean
    private AltchaService altchaService;

    @MockBean
    private SmtpPoolService smtpPoolService;

    @BeforeEach
    void setup() {
        logRepository.deleteAll();
        taskRepository.deleteAll();
        requestRepository.deleteAll();
        when(altchaService.verifyPayload("payload", "1.1.1.1")).thenReturn(true);
        when(smtpPoolService.hasAvailableSmtp()).thenReturn(true);
    }

    @Test
    void sendCreatesRequestAndTask() {
        var response = emailVerificationService.sendRegisterCode("tester", "tester@example.com", "1.1.1.1", "payload");
        assertThat(response.getRequestId()).isNotBlank();
        EmailVerificationRequest request = requestRepository.findById(response.getRequestId()).orElseThrow();
        assertThat(request.getStatus()).isEqualTo(EmailVerificationRequestStatus.PENDING);
        assertThat(request.getExpiresAt()).isAfter(LocalDateTime.now().minusMinutes(1));

        EmailSendTask task = taskRepository.findTopByRequestIdOrderByCreatedAtDesc(response.getRequestId());
        assertThat(task).isNotNull();
        EmailSendLog log = logRepository.findTopByRequestIdOrderByCreatedAtDesc(response.getRequestId());
        assertThat(log).isNotNull();
    }

    @Test
    void resendWithinIntervalThrows() {
        EmailVerificationRequest existing = EmailVerificationRequest.builder()
                .id("req-1")
                .username("tester")
                .email("tester@example.com")
                .ip("1.1.1.1")
                .codeHash(cryptoService.hashCode("123456"))
                .status(EmailVerificationRequestStatus.PENDING)
                .expiresAt(LocalDateTime.now().plus(properties.getCodeTtl()))
                .resendAvailableAt(LocalDateTime.now().plusMinutes(1))
                .createdAt(LocalDateTime.now())
                .build();
        requestRepository.save(existing);

        assertThatThrownBy(() -> emailVerificationService.sendRegisterCode("tester", "tester@example.com", "1.1.1.1", "payload"))
                .isInstanceOf(ResendNotReadyException.class);
    }

    @Test
    void verifyAndConsumeFlow() {
        String code = "654321";
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .id("req-verify")
                .username("tester")
                .email("tester@example.com")
                .ip("1.1.1.1")
                .codeHash(cryptoService.hashCode(code))
                .status(EmailVerificationRequestStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .resendAvailableAt(LocalDateTime.now().plusMinutes(1))
                .createdAt(LocalDateTime.now())
                .build();
        requestRepository.save(request);

        var verifyResponse = emailVerificationService.verifyCode("req-verify", "tester@example.com", code, "1.1.1.1");
        assertThat(verifyResponse.isVerified()).isTrue();

        EmailVerificationRequest updated = requestRepository.findById("req-verify").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(EmailVerificationRequestStatus.VERIFIED_PENDING_REGISTER);

        emailVerificationService.consumeForRegister("req-verify", "tester@example.com", code, "1.1.1.1", "tester");
        EmailVerificationRequest used = requestRepository.findById("req-verify").orElseThrow();
        assertThat(used.getStatus()).isEqualTo(EmailVerificationRequestStatus.USED);
    }
}
