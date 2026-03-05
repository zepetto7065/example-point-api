package com.musinsa.point.api.controller.payload.response;

import com.musinsa.point.domain.Transaction;
import com.musinsa.point.domain.enums.TransactionType;

import java.time.LocalDateTime;

public record TransactionResponse(
        Long transactionId,
        String pointKey,
        Long memberId,
        TransactionType type,
        Long amount,
        String orderId,
        String relatedPointKey,
        String description,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getTransactionId(),
                tx.getPointKey(),
                tx.getMemberId(),
                tx.getType(),
                tx.getAmount(),
                tx.getOrderId(),
                tx.getRelatedPointKey(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
