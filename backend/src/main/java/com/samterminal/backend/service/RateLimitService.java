package com.samterminal.backend.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private static class Bucket {
        private int count;
        private Instant windowStart;
    }

    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public boolean tryConsume(String key, int limit, Duration window) {
        if (limit <= 0) {
            return true;
        }
        Instant now = clock.instant();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        synchronized (bucket) {
            if (bucket.windowStart == null || Duration.between(bucket.windowStart, now).compareTo(window) >= 0) {
                bucket.windowStart = now;
                bucket.count = 0;
            }
            if (bucket.count >= limit) {
                return false;
            }
            bucket.count += 1;
            return true;
        }
    }

    public void reset(String key) {
        buckets.remove(key);
    }
}
