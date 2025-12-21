package com.samterminal.backend.service;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.entity.EmailSmtpConfig;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class JavaMailEmailSender implements EmailSender {
    private final EmailCryptoService cryptoService;
    private final EmailVerificationProperties properties;

    public JavaMailEmailSender(EmailCryptoService cryptoService, EmailVerificationProperties properties) {
        this.cryptoService = cryptoService;
        this.properties = properties;
    }

    @Override
    public void send(EmailSmtpConfig config, String to, String subject, String body) throws MailException {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        if (config.getPasswordEncrypted() != null && !config.getPasswordEncrypted().isBlank()) {
            sender.setPassword(cryptoService.decrypt(config.getPasswordEncrypted()));
        }
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", config.getUsername() != null && !config.getUsername().isBlank());
        props.put("mail.smtp.starttls.enable", config.isUseTls());
        props.put("mail.smtp.ssl.enable", config.isUseSsl());
        props.put("mail.smtp.connectiontimeout", properties.getSmtp().getConnectTimeoutSeconds() * 1000);
        props.put("mail.smtp.timeout", properties.getSmtp().getReadTimeoutSeconds() * 1000);
        props.put("mail.smtp.writetimeout", properties.getSmtp().getReadTimeoutSeconds() * 1000);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(config.getFromAddress());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
