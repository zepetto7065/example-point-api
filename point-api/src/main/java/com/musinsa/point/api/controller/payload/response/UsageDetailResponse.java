package com.musinsa.point.api.controller.payload.response;

import com.musinsa.point.domain.UsageDetail;

public record UsageDetailResponse(
        Long usageDetailId,
        Long useTransactionId,
        Long ledgerId,
        String ledgerPointKey,
        Long amount,
        Long cancelAmount,
        Long cancellableAmount
) {
    public static UsageDetailResponse from(UsageDetail detail) {
        return new UsageDetailResponse(
                detail.getUsageDetailId(),
                detail.getUseTransaction().getTransactionId(),
                detail.getLedger().getLedgerId(),
                detail.getLedger().getPointKey(),
                detail.getAmount(),
                detail.getCancelAmount(),
                detail.getCancellableAmount()
        );
    }
}
