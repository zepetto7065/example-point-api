package com.musinsa.point.api.controller.payload.response;

import com.musinsa.point.domain.Config;

public record ConfigResponse(
        Long configId,
        String configKey,
        String configValue,
        String description
) {
    public static ConfigResponse from(Config config) {
        return new ConfigResponse(
                config.getConfigId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getDescription()
        );
    }
}
