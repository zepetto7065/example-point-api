package com.musinsa.point.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class EarnDateTimeResolver {

    /**
     * 시작일을 결정합니다.
     * - null인 경우: 오늘 날짜 반환
     * - null이 아닌 경우: 입력값 그대로 반환
     *
     * @param startDate 시작일 (nullable)
     * @return 결정된 시작일
     */
    public LocalDate resolveStartDate(LocalDate startDate) {
        return startDate != null ? startDate : LocalDate.now();
    }

    /**
     * 만료일을 결정합니다.
     * - null인 경우: 시작일 + 기본 만료 일수
     * - null이 아닌 경우: 입력값 그대로 반환
     *
     * @param startDate 시작일
     * @param expireDate 만료일 (nullable)
     * @param defaultExpireDays 기본 만료 일수
     * @return 결정된 만료일
     */
    public LocalDate resolveExpireDate(LocalDate startDate, LocalDate expireDate, int defaultExpireDays) {
        if (expireDate != null) {
            return expireDate;
        }
        return startDate.plusDays(defaultExpireDays);
    }

    /**
     * 시작일을 시작 시각(00:00:00)으로 변환합니다.
     *
     * @param startDate 시작일
     * @return 시작 시각 (00:00:00)
     */
    public LocalDateTime calculateStartAt(LocalDate startDate) {
        return startDate.atStartOfDay();
    }

    /**
     * 만료일을 만료 시각(23:59:59)으로 변환합니다.
     *
     * @param expireDate 만료일
     * @return 만료 시각 (23:59:59)
     */
    public LocalDateTime calculateExpireAt(LocalDate expireDate) {
        return expireDate.atTime(23, 59, 59);
    }
}