package com.musinsa.point.validator;

import com.musinsa.point.exception.PointErrorCode;
import com.musinsa.point.exception.PointException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class EarnValidator {

    /**
     * 적립 금액의 유효성을 검증합니다.
     *
     * @param amount 적립 금액
     * @param maxEarnAmount 1회 최대 적립 가능 금액
     * @throws PointException 금액이 null이거나 1보다 작거나, 최대 적립 금액을 초과한 경우
     */
    public void validateEarnAmount(Long amount, Long maxEarnAmount) {
        if (amount == null || amount < 1) {
            throw new PointException(PointErrorCode.INVALID_EARN_AMOUNT);
        }
        if (amount > maxEarnAmount) {
            throw new PointException(PointErrorCode.EXCEED_MAX_EARN_AMOUNT);
        }
    }

    /**
     * 최대 잔액 한도를 검증합니다.
     *
     * @param currentBalance 현재 잔액
     * @param addAmount 추가할 금액
     * @param maxBalance 회원당 최대 보유 가능 잔액
     * @throws PointException 현재 잔액 + 추가 금액이 최대 보유 한도를 초과한 경우
     */
    public void validateMaxBalance(Long currentBalance, Long addAmount, Long maxBalance) {
        if (currentBalance + addAmount > maxBalance) {
            throw new PointException(PointErrorCode.EXCEED_MAX_BALANCE);
        }
    }

    /**
     * 날짜 범위의 유효성을 검증합니다.
     *
     * @param startDate 시작일
     * @param expireDate 만료일
     * @param minDays 최소 만료 일수
     * @param maxDays 최대 만료 일수
     * @throws PointException 만료일이 시작일 이전이거나, 날짜 간격이 허용 범위를 벗어난 경우
     */
    public void validateDateRange(LocalDate startDate, LocalDate expireDate, Long minDays, Long maxDays) {
        if (!expireDate.isAfter(startDate)) {
            throw new PointException(PointErrorCode.INVALID_EXPIRE_DATE);
        }
        long daysBetween = ChronoUnit.DAYS.between(startDate, expireDate);
        if (daysBetween < minDays || daysBetween > maxDays) {
            throw new PointException(PointErrorCode.INVALID_EXPIRE_DAYS);
        }
    }
}
