package com.musinsa.point.validator;

import com.musinsa.point.exception.PointErrorCode;
import com.musinsa.point.exception.PointException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EarnValidator 단위 테스트")
class EarnValidatorTest {

    private EarnValidator earnValidator;

    @BeforeEach
    void setUp() {
        earnValidator = new EarnValidator();
    }

    @Nested
    @DisplayName("적립 금액 검증")
    class ValidateEarnAmountTest {

        @Test
        @DisplayName("정상 금액 - 검증 성공")
        void validateEarnAmount_success() {
            assertThatCode(() -> earnValidator.validateEarnAmount(1000L, 10000L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("최소 금액(1) - 검증 성공")
        void validateEarnAmount_success_minAmount() {
            assertThatCode(() -> earnValidator.validateEarnAmount(1L, 10000L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("최대 금액과 동일 - 검증 성공")
        void validateEarnAmount_success_maxAmount() {
            assertThatCode(() -> earnValidator.validateEarnAmount(10000L, 10000L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("금액이 null - 검증 실패")
        void validateEarnAmount_fail_null() {
            assertThatThrownBy(() -> earnValidator.validateEarnAmount(null, 10000L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EARN_AMOUNT);
        }

        @Test
        @DisplayName("금액이 0 - 검증 실패")
        void validateEarnAmount_fail_zero() {
            assertThatThrownBy(() -> earnValidator.validateEarnAmount(0L, 10000L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EARN_AMOUNT);
        }

        @Test
        @DisplayName("금액이 음수 - 검증 실패")
        void validateEarnAmount_fail_negative() {
            assertThatThrownBy(() -> earnValidator.validateEarnAmount(-100L, 10000L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EARN_AMOUNT);
        }

        @Test
        @DisplayName("최대 금액 초과 - 검증 실패")
        void validateEarnAmount_fail_exceedMax() {
            assertThatThrownBy(() -> earnValidator.validateEarnAmount(10001L, 10000L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.EXCEED_MAX_EARN_AMOUNT);
        }
    }

    @Nested
    @DisplayName("최대 잔액 검증")
    class ValidateMaxBalanceTest {

        @Test
        @DisplayName("최대 잔액 이내 - 검증 성공")
        void validateMaxBalance_success() {
            assertThatCode(() -> earnValidator.validateMaxBalance(50000L, 30000L, 100000L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("최대 잔액과 정확히 같음 - 검증 성공")
        void validateMaxBalance_success_exactMax() {
            assertThatCode(() -> earnValidator.validateMaxBalance(90000L, 10000L, 100000L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("현재 잔액 0 - 검증 성공")
        void validateMaxBalance_success_zeroBalance() {
            assertThatCode(() -> earnValidator.validateMaxBalance(0L, 50000L, 100000L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("최대 잔액 초과 - 검증 실패")
        void validateMaxBalance_fail_exceedMax() {
            assertThatThrownBy(() -> earnValidator.validateMaxBalance(95000L, 6000L, 100000L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.EXCEED_MAX_BALANCE);
        }

        @Test
        @DisplayName("최대 잔액을 1 초과 - 검증 실패")
        void validateMaxBalance_fail_exceedByOne() {
            assertThatThrownBy(() -> earnValidator.validateMaxBalance(90000L, 10001L, 100000L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.EXCEED_MAX_BALANCE);
        }
    }

    @Nested
    @DisplayName("날짜 범위 검증")
    class ValidateDateRangeTest {

        @Test
        @DisplayName("정상 날짜 범위 - 검증 성공")
        void validateDateRange_success() {
            LocalDate startDate = LocalDate.now();
            LocalDate expireDate = startDate.plusDays(30);

            assertThatCode(() -> earnValidator.validateDateRange(startDate, expireDate, 1L, 730L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("최소 날짜 간격(1일) - 검증 성공")
        void validateDateRange_success_minDays() {
            LocalDate startDate = LocalDate.now();
            LocalDate expireDate = startDate.plusDays(1);

            assertThatCode(() -> earnValidator.validateDateRange(startDate, expireDate, 1L, 730L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("최대 날짜 간격(730일) - 검증 성공")
        void validateDateRange_success_maxDays() {
            LocalDate startDate = LocalDate.now();
            LocalDate expireDate = startDate.plusDays(730);

            assertThatCode(() -> earnValidator.validateDateRange(startDate, expireDate, 1L, 730L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("만료일이 시작일과 같음 - 검증 실패")
        void validateDateRange_fail_sameDate() {
            LocalDate date = LocalDate.now();

            assertThatThrownBy(() -> earnValidator.validateDateRange(date, date, 1L, 730L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EXPIRE_DATE);
        }

        @Test
        @DisplayName("만료일이 시작일 이전 - 검증 실패")
        void validateDateRange_fail_expireBeforeStart() {
            LocalDate startDate = LocalDate.now();
            LocalDate expireDate = startDate.minusDays(1);

            assertThatThrownBy(() -> earnValidator.validateDateRange(startDate, expireDate, 1L, 730L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EXPIRE_DATE);
        }

        @Test
        @DisplayName("최소 날짜 간격 미달 - 검증 실패")
        void validateDateRange_fail_belowMinDays() {
            LocalDate startDate = LocalDate.now();
            LocalDate expireDate = startDate.plusDays(6);

            assertThatThrownBy(() -> earnValidator.validateDateRange(startDate, expireDate, 7L, 730L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EXPIRE_DAYS);
        }

        @Test
        @DisplayName("최대 날짜 간격 초과 - 검증 실패")
        void validateDateRange_fail_exceedMaxDays() {
            LocalDate startDate = LocalDate.now();
            LocalDate expireDate = startDate.plusDays(731);

            assertThatThrownBy(() -> earnValidator.validateDateRange(startDate, expireDate, 1L, 730L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EXPIRE_DAYS);
        }

        @Test
        @DisplayName("최대 날짜 간격 1일 초과 - 검증 실패")
        void validateDateRange_fail_exceedMaxByOne() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate expireDate = LocalDate.of(2026, 1, 2); // 731일

            assertThatThrownBy(() -> earnValidator.validateDateRange(startDate, expireDate, 1L, 730L))
                    .isInstanceOf(PointException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EXPIRE_DAYS);
        }
    }
}