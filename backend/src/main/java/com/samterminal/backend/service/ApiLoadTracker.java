package com.samterminal.backend.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class ApiLoadTracker {
    private static final Duration WINDOW = Duration.ofSeconds(30);
    private final Map<Long, Deque<Instant>> calls = new ConcurrentHashMap<>();

    public void recordCall(Long apiId) {
        if (apiId == null) {
            return;
        }
        Deque<Instant> deque = calls.computeIfAbsent(apiId, key -> new ConcurrentLinkedDeque<>());
        Instant now = Instant.now();
        deque.addLast(now);
        prune(deque, now);
    }

    public int currentLoad(Long apiId) {
        if (apiId == null) {
            return 0;
        }
        Deque<Instant> deque = calls.computeIfAbsent(apiId, key -> new ConcurrentLinkedDeque<>());
        Instant now = Instant.now();
        prune(deque, now);
        return deque.size();
    }

    private void prune(Deque<Instant> deque, Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (true) {
            Instant head = deque.peekFirst();
            if (head == null || !head.isBefore(cutoff)) {
                return;
            }
            deque.pollFirst();
        }
    }
}
