package com.samterminal.backend.service;

import com.samterminal.backend.entity.EmailSmtpConfig;

public interface EmailSender {
    void send(EmailSmtpConfig config, String to, String subject, String body);
}
