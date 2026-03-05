package com.musinsa.point.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EarnDateTimeResolver 단위 테스트")
class EarnDateTimeResolverTest {

    private EarnDateTimeResolver dateTimeResolver;

    @BeforeEach
    void setUp() {
        dateTimeResolver = new EarnDateTimeResolver();
    }

    @Nested
    @DisplayName("시작일 결정")
    class ResolveStartDateTest {

        @Test
        @DisplayName("시작일이 null이면 오늘 날짜 반환")
        void resolveStartDate_withNull_returnsToday() {
            LocalDate result = dateTimeResolver.resolveStartDate(null);

            assertThat(result).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("시작일이 있으면 그대로 반환")
        void resolveStartDate_withValue_returnsSameValue() {
            LocalDate inputDate = LocalDate.of(2024, 6, 15);

            LocalDate result = dateTimeResolver.resolveStartDate(inputDate);

            assertThat(result).isEqualTo(inputDate);
        }
    }

    @Nested
    @DisplayName("만료일 결정")
    class ResolveExpireDateTest {

        @Test
        @DisplayName("만료일이 null이면 시작일 + 기본 일수 반환")
        void resolveExpireDate_withNull_returnsStartDatePlusDefaultDays() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            int defaultDays = 365;

            LocalDate result = dateTimeResolver.resolveExpireDate(startDate, null, defaultDays);

            assertThat(result).isEqualTo(LocalDate.of(2024, 12, 31));
        }

        @Test
        @DisplayName("만료일이 있으면 그대로 반환")
        void resolveExpireDate_withValue_returnsSameValue() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate expireDate = LocalDate.of(2024, 6, 30);
            int defaultDays = 365;

            LocalDate result = dateTimeResolver.resolveExpireDate(startDate, expireDate, defaultDays);

            assertThat(result).isEqualTo(expireDate);
        }

        @Test
        @DisplayName("기본 일수가 30일인 경우")
        void resolveExpireDate_with30Days() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);

            LocalDate result = dateTimeResolver.resolveExpireDate(startDate, null, 30);

            assertThat(result).isEqualTo(LocalDate.of(2024, 1, 31));
        }

        @Test
        @DisplayName("기본 일수가 1일인 경우")
        void resolveExpireDate_with1Day() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);

            LocalDate result = dateTimeResolver.resolveExpireDate(startDate, null, 1);

            assertThat(result).isEqualTo(LocalDate.of(2024, 1, 2));
        }
    }

    @Nested
    @DisplayName("시작 시각 계산")
    class CalculateStartAtTest {

        @Test
        @DisplayName("시작일을 00:00:00으로 변환")
        void calculateStartAt_returnsStartOfDay() {
            LocalDate startDate = LocalDate.of(2024, 6, 15);

            LocalDateTime result = dateTimeResolver.calculateStartAt(startDate);

            assertThat(result).isEqualTo(LocalDateTime.of(2024, 6, 15, 0, 0, 0));
        }

        @Test
        @DisplayName("오늘 날짜를 00:00:00으로 변환")
        void calculateStartAt_today() {
            LocalDate today = LocalDate.now();

            LocalDateTime result = dateTimeResolver.calculateStartAt(today);

            assertThat(result).isEqualTo(today.atStartOfDay());
        }
    }

    @Nested
    @DisplayName("만료 시각 계산")
    class CalculateExpireAtTest {

        @Test
        @DisplayName("만료일을 23:59:59로 변환")
        void calculateExpireAt_returnsEndOfDay() {
            LocalDate expireDate = LocalDate.of(2024, 12, 31);

            LocalDateTime result = dateTimeResolver.calculateExpireAt(expireDate);

            assertThat(result).isEqualTo(LocalDateTime.of(2024, 12, 31, 23, 59, 59));
        }

        @Test
        @DisplayName("오늘 날짜를 23:59:59로 변환")
        void calculateExpireAt_today() {
            LocalDate today = LocalDate.now();

            LocalDateTime result = dateTimeResolver.calculateExpireAt(today);

            assertThat(result).isEqualTo(today.atTime(23, 59, 59));
        }
    }
}