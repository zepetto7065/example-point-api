package com.musinsa.point.api.controller.payload.request;

import jakarta.validation.constraints.NotBlank;

public record ConfigUpdateRequest(
        @NotBlank(message = "설정값은 필수입니다.")
        String configValue
) {
}
