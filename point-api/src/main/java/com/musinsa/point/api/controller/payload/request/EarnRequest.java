package com.musinsa.point.api.controller.payload.request;

import com.musinsa.point.domain.enums.EarnType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EarnRequest(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @NotNull(message = "적립 금액은 필수입니다.")
        @Min(value = 1, message = "적립 금액은 1 이상이어야 합니다.")
        Long amount,

        @NotNull(message = "적립 유형은 필수입니다.")
        EarnType earnType,

        String description,
        String userId,
        LocalDate startDate,
        LocalDate expireDate
) {
}
