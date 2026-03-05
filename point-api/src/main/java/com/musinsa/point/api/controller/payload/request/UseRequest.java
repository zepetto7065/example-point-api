package com.musinsa.point.api.controller.payload.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UseRequest(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @NotNull(message = "사용 금액은 필수입니다.")
        @Min(value = 1, message = "사용 금액은 1 이상이어야 합니다.")
        Long amount,

        @NotBlank(message = "주문 ID는 필수입니다.")
        String orderId,

        String description
) {
}
