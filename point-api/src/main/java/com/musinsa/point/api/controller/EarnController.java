package com.musinsa.point.api.controller;

import com.musinsa.point.api.controller.payload.request.EarnRequest;
import com.musinsa.point.api.controller.payload.response.EarnResponse;
import com.musinsa.point.api.service.EarnFacadeService;
import com.musinsa.point.service.EarnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points/earn")
@RequiredArgsConstructor
public class EarnController {

    private final EarnFacadeService earnFacadeService;
    private final EarnService earnService;

    @PostMapping
    public ResponseEntity<EarnResponse> earn(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody EarnRequest request) {

        EarnResponse response = earnFacadeService.earn(
                idempotencyKey,
                request.memberId(), request.amount(), request.earnType(),
                request.description(), request.userId(),
                request.startDate(), request.expireDate());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{pointKey}")
    public ResponseEntity<Void> cancelEarn(@PathVariable String pointKey) {
        earnService.cancelEarn(pointKey);
        return ResponseEntity.noContent().build();
    }
}
