package org.feiesos.auth.service.impl;

import org.feiesos.auth.service.RateLimitService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitServiceImpl implements RateLimitService {

    private final Map<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean isRateLimited(String key, int maxAttempts, Duration window) {
        Deque<Instant> attempts = buckets.getOrDefault(key, new ArrayDeque<>());
        Instant cutoff = Instant.now().minus(window);

        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }

        return attempts.size() >= maxAttempts;
    }

    @Override
    public void recordAttempt(String key) {
        Deque<Instant> attempts = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        attempts.addLast(Instant.now());
    }

    @Override
    public void clearAttempts(String key) {
        buckets.remove(key);
    }
}
