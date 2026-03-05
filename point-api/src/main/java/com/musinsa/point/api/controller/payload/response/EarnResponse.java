package com.musinsa.point.api.controller.payload.response;

import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.LedgerStatus;

import java.time.LocalDateTime;

public record EarnResponse(
        Long ledgerId,
        String pointKey,
        Long memberId,
        Long earnAmount,
        Long balance,
        EarnType earnType,
        String description,
        LedgerStatus status,
        LocalDateTime startAt,
        LocalDateTime expireAt,
        LocalDateTime createdAt
) {
    public static EarnResponse from(Ledger ledger) {
        return new EarnResponse(
                ledger.getLedgerId(),
                ledger.getPointKey(),
                ledger.getMemberId(),
                ledger.getEarnAmount(),
                ledger.getBalance(),
                ledger.getEarnType(),
                ledger.getDescription(),
                ledger.getStatus(),
                ledger.getStartAt(),
                ledger.getExpireAt(),
                ledger.getCreatedAt()
        );
    }
}
