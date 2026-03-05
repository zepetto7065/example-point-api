package com.musinsa.point.api.component;

import com.musinsa.point.domain.Transaction;

@FunctionalInterface
public interface IdempotencyResponseResolver<T> {

    T resolve(Transaction transaction);
}
