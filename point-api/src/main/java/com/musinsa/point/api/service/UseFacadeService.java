package com.musinsa.point.api.service;

import com.musinsa.point.api.annotation.Idempotent;
import com.musinsa.point.api.annotation.IdempotencyKey;
import com.musinsa.point.api.component.TransactionResponseResolver;
import com.musinsa.point.api.controller.payload.response.TransactionResponse;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.service.UseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UseFacadeService {

    private final UseService useService;

    @Idempotent(resolver = TransactionResponseResolver.class)
    public TransactionResponse use(@IdempotencyKey String idempotencyKey, Long memberId, Long amount,
                                   String orderId, String description) {
        Transaction transaction = useService.use(memberId, amount, orderId, description, idempotencyKey);
        return TransactionResponse.from(transaction);
    }

    @Idempotent(resolver = TransactionResponseResolver.class)
    public TransactionResponse cancelUse(@IdempotencyKey String idempotencyKey, String usePointKey, Long cancelAmount) {
        Transaction transaction = useService.cancelUse(usePointKey, cancelAmount, idempotencyKey);
        return TransactionResponse.from(transaction);
    }
}
