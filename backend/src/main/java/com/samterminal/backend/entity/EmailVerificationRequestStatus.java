package com.samterminal.backend.entity;

public enum EmailVerificationRequestStatus {
    PENDING,
    VERIFIED_PENDING_REGISTER,
    USED,
    SUPERSEDED,
    EXPIRED
}
