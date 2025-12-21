package com.samterminal.backend.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class ResendNotReadyException extends ApiException {
    private final LocalDateTime resendAvailableAt;

    public ResendNotReadyException(LocalDateTime resendAvailableAt) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RESEND_NOT_READY", "重发间隔未到");
        this.resendAvailableAt = resendAvailableAt;
    }

    public LocalDateTime getResendAvailableAt() {
        return resendAvailableAt;
    }
}
