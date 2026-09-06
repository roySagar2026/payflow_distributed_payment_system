package com.payflow.payflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PROCESSING_MARKER = "PROCESSING";

    public <T> Optional<T> reserveOrGetCached(String idempotencyKey, Object requestBody, Class<T> responseType) {
        String requestHash = hashRequest(requestBody);
        String storedHashKey = "idempotency:hash:" + idempotencyKey;
        String storedResponseKey = "idempotency:response:" + idempotencyKey;

        String existingHash = redisTemplate.opsForValue().get(storedHashKey);

        if (existingHash == null) {
            boolean reserved = Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(storedHashKey, requestHash, TTL)
            );
            if (!reserved) {
                throw new IllegalStateException("Request with this idempotency key is already being processed");
            }
            redisTemplate.opsForValue().set(storedResponseKey, PROCESSING_MARKER, TTL);
            return Optional.empty();
        }

        if (!existingHash.equals(requestHash)) {
            throw new IllegalArgumentException("Idempotency key reused with a different request body");
        }

        String storedResponse = redisTemplate.opsForValue().get(storedResponseKey);

        if (PROCESSING_MARKER.equals(storedResponse)) {
            throw new IllegalStateException("Request with this idempotency key is still being processed");
        }

        // Jackson 3's readValue throws unchecked exceptions — no try/catch needed here
        return Optional.of(jsonMapper.readValue(storedResponse, responseType));
    }

    public void markCompleted(String idempotencyKey, Object response) {
        String storedResponseKey = "idempotency:response:" + idempotencyKey;
        String json = jsonMapper.writeValueAsString(response);
        redisTemplate.opsForValue().set(storedResponseKey, json, TTL);
    }

    private String hashRequest(Object requestBody) {
        try {
            String json = jsonMapper.writeValueAsString(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash request", e);
        }
    }
}