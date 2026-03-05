package com.musinsa.point.service;

import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.UsageDetail;
import com.musinsa.point.repository.LedgerRepository;
import com.musinsa.point.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpireService {

    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public int expirePoints() {
        List<Ledger> expiredLedgers = ledgerRepository.findExpiredLedgers(LocalDateTime.now());
        int count = 0;

        for (Ledger ledger : expiredLedgers) {
            long remainingBalance = ledger.getBalance();
            if (remainingBalance <= 0) continue;

            walletRepository.findByMemberId(ledger.getMemberId())
                    .ifPresent(wallet -> wallet.subtractBalance(remainingBalance));

            ledger.expire();
            count++;
            log.info("포인트 만료 처리: ledgerId={}, memberId={}, amount={}",
                    ledger.getLedgerId(), ledger.getMemberId(), remainingBalance);
        }

        return count;
    }
}
