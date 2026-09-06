package com.payflow.payflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Returns true if the request is ALLOWED, false if the rate limit was exceeded.
     *
     * @param key        unique identifier for what's being limited (e.g., "ratelimit:withdraw:userId")
     * @param maxRequests maximum allowed requests in the window
     * @param window      the time window duration
     */
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        // Bucket requests into fixed windows by truncating the current time to the window size.
        // e.g., for a 1-minute window, all requests within the same clock-minute share a bucket.
        long windowSeconds = window.getSeconds();
        long currentWindow = Instant.now().getEpochSecond() / windowSeconds;
        String windowKey = key + ":" + currentWindow;

        Long count = redisTemplate.opsForValue().increment(windowKey);

        if (count != null && count == 1L) {
            // First request in this window — set expiry so old windows clean themselves up
            redisTemplate.expire(windowKey, window);
        }

        return count != null && count <= maxRequests;
    }
}