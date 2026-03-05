package com.musinsa.point.api.controller.payload.response;

import com.musinsa.point.domain.Wallet;

public record BalanceResponse(
        Long memberId,
        Long totalBalance
) {
    public static BalanceResponse from(Wallet wallet) {
        return new BalanceResponse(wallet.getMemberId(), wallet.getTotalBalance());
    }
}
