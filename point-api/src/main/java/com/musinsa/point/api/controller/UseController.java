package com.musinsa.point.api.controller;

import com.musinsa.point.api.controller.payload.request.UseCancelRequest;
import com.musinsa.point.api.controller.payload.request.UseRequest;
import com.musinsa.point.api.controller.payload.response.TransactionResponse;
import com.musinsa.point.api.service.UseFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class UseController {

    private final UseFacadeService useFacadeService;

    @PostMapping("/use")
    public ResponseEntity<TransactionResponse> use(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody UseRequest request) {

        TransactionResponse response = useFacadeService.use(
                idempotencyKey,
                request.memberId(), request.amount(), request.orderId(), request.description());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/use-cancel")
    public ResponseEntity<TransactionResponse> cancelUse(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody UseCancelRequest request) {

        TransactionResponse response = useFacadeService.cancelUse(
                idempotencyKey, request.usePointKey(), request.cancelAmount());

        return ResponseEntity.ok(response);
    }
}
