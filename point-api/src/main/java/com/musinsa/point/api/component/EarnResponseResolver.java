package com.musinsa.point.api.component;

import com.musinsa.point.api.controller.payload.response.EarnResponse;
import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EarnResponseResolver implements IdempotencyResponseResolver<EarnResponse> {

    private final LedgerRepository ledgerRepository;

    @Override
    public EarnResponse resolve(Transaction transaction) {
        Ledger ledger = ledgerRepository.findByPointKey(transaction.getRelatedPointKey())
                .orElseThrow();
        return EarnResponse.from(ledger);
    }
}
