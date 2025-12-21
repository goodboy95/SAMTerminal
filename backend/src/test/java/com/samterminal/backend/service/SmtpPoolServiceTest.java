package com.samterminal.backend.service;

import com.samterminal.backend.entity.EmailSmtpConfig;
import com.samterminal.backend.repository.EmailSmtpConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class SmtpPoolServiceTest {

    @Autowired
    private SmtpPoolService smtpPoolService;

    @Autowired
    private EmailSmtpConfigRepository repository;

    @MockBean
    private EmailSender emailSender;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void fallsBackOnFailure() {
        EmailSmtpConfig failing = EmailSmtpConfig.builder()
                .name("fail")
                .host("fail.host")
                .port(587)
                .fromAddress("noreply@example.com")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        EmailSmtpConfig ok = EmailSmtpConfig.builder()
                .name("ok")
                .host("ok.host")
                .port(587)
                .fromAddress("noreply@example.com")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        repository.save(failing);
        repository.save(ok);

        doThrow(new MailSendException("fail"))
                .when(emailSender).send(Mockito.argThat(cfg -> "fail.host".equals(cfg.getHost())), anyString(), anyString(), anyString());
        doAnswer(invocation -> null)
                .when(emailSender).send(Mockito.argThat(cfg -> "ok.host".equals(cfg.getHost())), anyString(), anyString(), anyString());

        EmailSmtpConfig used = smtpPoolService.sendWithFailover("user@example.com", "subject", "body");
        assertThat(used.getHost()).isEqualTo("ok.host");
    }
}
