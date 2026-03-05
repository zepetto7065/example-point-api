package com.musinsa.point.api.controller.payload.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UseCancelRequest(
        @NotBlank(message = "사용 거래 pointKey는 필수입니다.")
        String usePointKey,

        @NotNull(message = "취소 금액은 필수입니다.")
        @Min(value = 1, message = "취소 금액은 1 이상이어야 합니다.")
        Long cancelAmount
) {
}
