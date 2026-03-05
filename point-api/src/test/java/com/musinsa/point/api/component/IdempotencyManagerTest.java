package com.musinsa.point.api.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IdempotencyManager 단위 테스트")
class IdempotencyManagerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyManager idempotencyManager;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("findCachedResponse - Redis에 값이 있으면 역직렬화하여 반환")
    void findCachedResponse_whenValueExists_returnsDeserialized() throws Exception {
        // given
        String idempotencyKey = "test-key";
        String cachedJson = "{\"id\":1,\"amount\":100}";
        TestResponse expectedResponse = new TestResponse(1L, 100);

        when(valueOperations.get("idempotency:test-key")).thenReturn(cachedJson);
        when(objectMapper.readValue(cachedJson, TestResponse.class)).thenReturn(expectedResponse);

        // when
        Optional<TestResponse> result = idempotencyManager.findCachedResponse(idempotencyKey, TestResponse.class);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("findCachedResponse - Redis에 값이 없으면 Optional.empty() 반환")
    void findCachedResponse_whenValueNotExists_returnsEmpty() {
        // given
        String idempotencyKey = "test-key";
        when(valueOperations.get("idempotency:test-key")).thenReturn(null);

        // when
        Optional<TestResponse> result = idempotencyManager.findCachedResponse(idempotencyKey, TestResponse.class);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findCachedResponse - Redis 예외 발생 시 Optional.empty() 반환하고 에러 무시")
    void findCachedResponse_whenRedisThrowsException_returnsEmpty() {
        // given
        String idempotencyKey = "test-key";
        when(valueOperations.get("idempotency:test-key")).thenThrow(new RuntimeException("Redis error"));

        // when
        Optional<TestResponse> result = idempotencyManager.findCachedResponse(idempotencyKey, TestResponse.class);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findInDb - idempotencyKey로 Transaction 조회")
    void findInDb_returnsTransactionFromRepository() {
        // given
        String idempotencyKey = "test-key";
        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .build();

        when(transactionRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(transaction));

        // when
        Optional<Transaction> result = idempotencyManager.findInDb(idempotencyKey);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getIdempotencyKey()).isEqualTo(idempotencyKey);
    }

    @Test
    @DisplayName("cacheResponse - 직렬화 후 Redis에 TTL과 함께 저장")
    void cacheResponse_serializesAndStoresInRedis() throws Exception {
        // given
        String idempotencyKey = "test-key";
        TestResponse response = new TestResponse(1L, 100);
        String serializedJson = "{\"id\":1,\"amount\":100}";

        when(objectMapper.writeValueAsString(response)).thenReturn(serializedJson);

        // when
        idempotencyManager.cacheResponse(idempotencyKey, response);

        // then
        verify(objectMapper).writeValueAsString(response);
        verify(valueOperations).set("idempotency:test-key", serializedJson, Duration.ofHours(24));
    }

    @Test
    @DisplayName("cacheResponse - Redis 예외 발생 시 에러 무시하고 예외 던지지 않음")
    void cacheResponse_whenRedisThrowsException_doesNotThrow() throws Exception {
        // given
        String idempotencyKey = "test-key";
        TestResponse response = new TestResponse(1L, 100);
        String serializedJson = "{\"id\":1,\"amount\":100}";

        when(objectMapper.writeValueAsString(response)).thenReturn(serializedJson);
        doThrow(new RuntimeException("Redis error"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // when & then
        assertThatCode(() -> idempotencyManager.cacheResponse(idempotencyKey, response))
                .doesNotThrowAnyException();
    }

    private record TestResponse(Long id, Integer amount) {}
}
