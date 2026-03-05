package com.musinsa.point.api.component;

import com.musinsa.point.api.controller.payload.response.TransactionResponse;
import com.musinsa.point.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionResponseResolver implements IdempotencyResponseResolver<TransactionResponse> {

    @Override
    public TransactionResponse resolve(Transaction transaction) {
        return TransactionResponse.from(transaction);
    }
}
