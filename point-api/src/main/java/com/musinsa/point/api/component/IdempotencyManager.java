package com.musinsa.point.api.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyManager {

    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> findCachedResponse(String idempotencyKey, Class<T> responseType) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
            if (value != null) {
                return Optional.of(objectMapper.readValue(value, responseType));
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for idempotency key: {}", idempotencyKey, e);
        }
        return Optional.empty();
    }

    public Optional<Transaction> findInDb(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey);
    }

    public <T> void cacheResponse(String idempotencyKey, T response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, json, TTL);
        } catch (Exception e) {
            log.warn("Redis cache failed for idempotency key: {}", idempotencyKey, e);
        }
    }
}