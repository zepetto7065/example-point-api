package com.musinsa.point.api.controller;

import com.musinsa.point.api.controller.payload.request.ConfigUpdateRequest;
import com.musinsa.point.api.controller.payload.response.ConfigResponse;
import com.musinsa.point.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/points/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public ResponseEntity<List<ConfigResponse>> getAllConfigs() {
        return ResponseEntity.ok(
                configService.getAllConfigs().stream()
                        .map(ConfigResponse::from).toList());
    }

    @PutMapping("/{configKey}")
    public ResponseEntity<ConfigResponse> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody ConfigUpdateRequest request) {
        return ResponseEntity.ok(
                ConfigResponse.from(configService.updateConfig(configKey, request.configValue())));
    }
}
