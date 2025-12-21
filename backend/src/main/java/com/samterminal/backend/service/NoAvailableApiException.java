package com.samterminal.backend.service;

public class NoAvailableApiException extends RuntimeException {
    public NoAvailableApiException(String message) {
        super(message);
    }
}
