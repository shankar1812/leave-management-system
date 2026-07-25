package com.app.leaveManagement.service.impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import com.app.leaveManagement.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimitServiceImpl implements RateLimitService {

    // One bucket per user – thread-safe map
    private final ConcurrentHashMap<Long, Bucket> buckets = new ConcurrentHashMap<>();

    // Each user gets 3 tokens
    // Refills 1 token every 20 minutes = 3 tokens per hour max
    private static final int CAPACITY          = 3;
    private static final int REFILL_TOKENS     = 1;
    private static final int REFILL_MINUTES    = 20;

    @Override
    public boolean tryConsume(Long userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, this::createNewBucket);
        boolean consumed = bucket.tryConsume(1);

        log.debug("Rate limit check for user id: {} — consumed: {}, remaining: {}",
                userId, consumed, bucket.getAvailableTokens());

        return consumed;
    }

    @Override
    public long getRemainingTokens(Long userId) {
        Bucket bucket = buckets.get(userId);
        return bucket != null ? bucket.getAvailableTokens() : CAPACITY;
    }

    private Bucket createNewBucket(Long userId) {
        log.debug("Creating new rate limit bucket for user id: {}", userId);

        Bandwidth limit = Bandwidth.classic(
                CAPACITY,
                Refill.intervally(REFILL_TOKENS, Duration.ofMinutes(REFILL_MINUTES))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}