package com.musinsa.point.api.annotation;

import com.musinsa.point.api.component.IdempotencyResponseResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    Class<? extends IdempotencyResponseResolver<?>> resolver();
}
