package com.app.leaveManagement.service;

public interface RateLimitService {

    boolean tryConsume(Long userId);

    long getRemainingTokens(Long userId);
}