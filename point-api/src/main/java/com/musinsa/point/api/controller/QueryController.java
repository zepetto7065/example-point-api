package com.musinsa.point.api.controller;

import com.musinsa.point.api.controller.payload.response.BalanceResponse;
import com.musinsa.point.api.controller.payload.response.LedgerResponse;
import com.musinsa.point.api.controller.payload.response.TransactionResponse;
import com.musinsa.point.api.controller.payload.response.UsageDetailResponse;
import com.musinsa.point.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @GetMapping("/balance/{memberId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long memberId) {
        return ResponseEntity.ok(BalanceResponse.from(queryService.getBalance(memberId)));
    }

    @GetMapping("/transactions/{memberId}")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @PathVariable Long memberId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                queryService.getTransactions(memberId, pageable).map(TransactionResponse::from));
    }

    @GetMapping("/ledgers/{memberId}")
    public ResponseEntity<Page<LedgerResponse>> getLedgers(
            @PathVariable Long memberId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                queryService.getLedgers(memberId, pageable).map(LedgerResponse::from));
    }

    @GetMapping("/usage-details/{orderId}")
    public ResponseEntity<List<UsageDetailResponse>> getUsageDetails(@PathVariable String orderId) {
        return ResponseEntity.ok(
                queryService.getUsageDetailsByOrderId(orderId).stream()
                        .map(UsageDetailResponse::from).toList());
    }
}
