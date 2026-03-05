package com.musinsa.point.api.component;

import com.musinsa.point.api.annotation.Idempotent;
import com.musinsa.point.api.annotation.IdempotencyKey;
import com.musinsa.point.domain.Transaction;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyManager idempotencyManager;
    private final ApplicationContext applicationContext;

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String idempotencyKey = extractIdempotencyKey(joinPoint);
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();

        Optional<?> cached = idempotencyManager.findCachedResponse(idempotencyKey, returnType);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<Transaction> existing = idempotencyManager.findInDb(idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyResponseResolver<?> resolver = applicationContext.getBean(idempotent.resolver());
            Object response = resolver.resolve(existing.get());
            idempotencyManager.cacheResponse(idempotencyKey, response);
            return response;
        }

        Object result = joinPoint.proceed();

        idempotencyManager.cacheResponse(idempotencyKey, result);

        return result;
    }

    private String extractIdempotencyKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(IdempotencyKey.class)) {
                return (String) args[i];
            }
        }
        throw new IllegalStateException("@IdempotencyKey parameter not found");
    }
}
