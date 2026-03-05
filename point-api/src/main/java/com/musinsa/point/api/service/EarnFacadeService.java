package com.musinsa.point.api.service;

import com.musinsa.point.api.annotation.Idempotent;
import com.musinsa.point.api.annotation.IdempotencyKey;
import com.musinsa.point.api.component.EarnResponseResolver;
import com.musinsa.point.api.controller.payload.response.EarnResponse;
import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.service.EarnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EarnFacadeService {

    private final EarnService earnService;

    @Idempotent(resolver = EarnResponseResolver.class)
    public EarnResponse earn(@IdempotencyKey String idempotencyKey, Long memberId, Long amount, EarnType earnType,
                             String description, String userId,
                             LocalDate startDate, LocalDate expireDate) {
        Ledger ledger = earnService.earn(memberId, amount, earnType, description, userId,
                startDate, expireDate, idempotencyKey);
        return EarnResponse.from(ledger);
    }
}
