package org.feiesos.auth.service;

import java.time.Duration;

public interface RateLimitService {

    boolean isRateLimited(String key, int maxAttempts, Duration window);

    void recordAttempt(String key);

    void clearAttempts(String key);
}
