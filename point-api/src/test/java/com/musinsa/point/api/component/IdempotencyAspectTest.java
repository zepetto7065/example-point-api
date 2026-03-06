package com.musinsa.point.api.component;

import com.musinsa.point.api.annotation.Idempotent;
import com.musinsa.point.api.annotation.IdempotencyKey;
import com.musinsa.point.domain.Transaction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

import com.musinsa.point.exception.PointErrorCode;
import com.musinsa.point.exception.PointException;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IdempotencyAspect 단위 테스트")
class IdempotencyAspectTest {

    @Mock
    private IdempotencyManager idempotencyManager;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private IdempotencyResponseResolver<String> resolver;

    @InjectMocks
    private IdempotencyAspect idempotencyAspect;

    private Idempotent idempotent;

    @BeforeEach
    void setUp() {
        idempotent = mock(Idempotent.class);
        when(idempotent.resolver()).thenReturn((Class) IdempotencyResponseResolver.class);
    }

    @Test
    @DisplayName("캐시에 응답이 있으면 캐시된 응답을 반환하고 proceed를 호출하지 않는다")
    void shouldReturnCachedResponseWhenCacheHit() throws Throwable {
        // given
        String idempotencyKey = "test-key-123";
        String cachedResponse = "cached-response";
        Method method = TestService.class.getMethod("testMethod", String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(String.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{idempotencyKey});
        when(idempotencyManager.findCachedResponse(idempotencyKey, String.class))
                .thenReturn(Optional.of(cachedResponse));

        // when
        Object result = idempotencyAspect.handleIdempotency(joinPoint, idempotent);

        // then
        assertThat(result).isEqualTo(cachedResponse);
        verify(joinPoint, never()).proceed();
        verify(idempotencyManager, never()).findInDb(any());
        verify(idempotencyManager, never()).cacheResponse(any(), any());
    }

    @Test
    @DisplayName("캐시 미스이지만 DB에 트랜잭션이 있으면 resolver로 응답을 복원하고 캐싱한다")
    void shouldResolveFromDbAndCacheWhenCacheMissButDbHit() throws Throwable {
        // given
        String idempotencyKey = "test-key-456";
        Transaction transaction = Transaction.builder().build();
        String resolvedResponse = "resolved-response";
        Method method = TestService.class.getMethod("testMethod", String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(String.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{idempotencyKey});
        when(idempotencyManager.findCachedResponse(idempotencyKey, String.class))
                .thenReturn(Optional.empty());
        when(idempotencyManager.findInDb(idempotencyKey))
                .thenReturn(Optional.of(transaction));
        when(applicationContext.getBean(IdempotencyResponseResolver.class))
                .thenReturn(resolver);
        when(resolver.resolve(transaction)).thenReturn(resolvedResponse);

        // when
        Object result = idempotencyAspect.handleIdempotency(joinPoint, idempotent);

        // then
        assertThat(result).isEqualTo(resolvedResponse);
        verify(joinPoint, never()).proceed();
        verify(resolver).resolve(transaction);
        verify(idempotencyManager).cacheResponse(idempotencyKey, resolvedResponse);
    }

    @Test
    @DisplayName("캐시와 DB 모두 미스이면 proceed를 실행하고 결과를 캐싱한다")
    void shouldProceedAndCacheWhenCacheMissAndDbMiss() throws Throwable {
        // given
        String idempotencyKey = "test-key-789";
        String proceedResult = "new-response";
        Method method = TestService.class.getMethod("testMethod", String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(String.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{idempotencyKey});
        when(idempotencyManager.findCachedResponse(idempotencyKey, String.class))
                .thenReturn(Optional.empty());
        when(idempotencyManager.findInDb(idempotencyKey))
                .thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenReturn(proceedResult);

        // when
        Object result = idempotencyAspect.handleIdempotency(joinPoint, idempotent);

        // then
        assertThat(result).isEqualTo(proceedResult);
        verify(joinPoint).proceed();
        verify(idempotencyManager).cacheResponse(idempotencyKey, proceedResult);
        verify(applicationContext, never()).getBean(any(Class.class));
    }

    @Test
    @DisplayName("@IdempotencyKey 어노테이션이 붙은 파라미터가 없으면 IllegalStateException을 발생시킨다")
    void shouldThrowExceptionWhenIdempotencyKeyParameterNotFound() throws Throwable {
        // given
        Method method = TestService.class.getMethod("methodWithoutIdempotencyKey", String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"some-value"});

        // when & then
        assertThatThrownBy(() -> idempotencyAspect.handleIdempotency(joinPoint, idempotent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("@IdempotencyKey parameter not found");
    }

    @Test
    @DisplayName("proceed 중 DataIntegrityViolationException 발생 시 DUPLICATE_REQUEST 예외를 던진다")
    void shouldThrowDuplicateRequestWhenDataIntegrityViolationOccurs() throws Throwable {
        // given
        String idempotencyKey = "race-condition-key";
        Method method = TestService.class.getMethod("testMethod", String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(String.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{idempotencyKey});
        when(idempotencyManager.findCachedResponse(idempotencyKey, String.class))
                .thenReturn(Optional.empty());
        when(idempotencyManager.findInDb(idempotencyKey))
                .thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenThrow(new DataIntegrityViolationException("Unique constraint violated"));

        // when & then
        assertThatThrownBy(() -> idempotencyAspect.handleIdempotency(joinPoint, idempotent))
                .isInstanceOf(PointException.class)
                .extracting(e -> ((PointException) e).getErrorCode())
                .isEqualTo(PointErrorCode.DUPLICATE_REQUEST);
    }

    static class TestService {
        public String testMethod(@IdempotencyKey String key) {
            return "result";
        }

        public String methodWithoutIdempotencyKey(String key) {
            return "result";
        }
    }
}
