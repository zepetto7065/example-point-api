package com.musinsa.point.service;

import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.domain.UsageDetail;
import com.musinsa.point.domain.Wallet;
import com.musinsa.point.domain.enums.ConfigKey;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.TransactionType;
import com.musinsa.point.exception.PointErrorCode;
import com.musinsa.point.exception.PointException;
import com.musinsa.point.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UseService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final UsageDetailRepository usageDetailRepository;
    private final ConfigService configService;

    @Transactional
    public Transaction use(Long memberId, Long amount, String orderId, String description,
                           String idempotencyKey) {
        if (amount == null || amount < 1) {
            throw new PointException(PointErrorCode.INVALID_USE_AMOUNT);
        }

        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new PointException(PointErrorCode.WALLET_NOT_FOUND));

        if (wallet.getTotalBalance() < amount) {
            throw new PointException(PointErrorCode.INSUFFICIENT_BALANCE);
        }

        // 차감 순서: ADMIN_MANUAL 우선, 만료일 짧은 순
        List<Ledger> ledgers = ledgerRepository.findDeductibleLedgers(memberId, LocalDateTime.now());

        Transaction transaction = Transaction.builder()
                .memberId(memberId)
                .type(TransactionType.USE)
                .amount(amount)
                .orderId(orderId)
                .description(description)
                .idempotencyKey(idempotencyKey)
                .build();
        transactionRepository.save(transaction);

        long remaining = amount;
        List<UsageDetail> details = new ArrayList<>();

        for (Ledger ledger : ledgers) {
            if (remaining <= 0) break;

            long deduct = Math.min(remaining, ledger.getAvailableBalance());
            if (deduct <= 0) continue;

            ledger.use(deduct);
            remaining -= deduct;

            UsageDetail detail = UsageDetail.builder()
                    .useTransaction(transaction)
                    .ledger(ledger)
                    .amount(deduct)
                    .build();
            details.add(detail);
        }

        if (remaining > 0) {
            throw new PointException(PointErrorCode.INSUFFICIENT_BALANCE);
        }

        usageDetailRepository.saveAll(details);
        wallet.subtractBalance(amount);

        return transaction;
    }

    @Transactional
    public Transaction cancelUse(String usePointKey, Long cancelAmount, String idempotencyKey) {
        Transaction useTransaction = transactionRepository.findByPointKey(usePointKey)
                .orElseThrow(() -> new PointException(PointErrorCode.TRANSACTION_NOT_FOUND));

        if (useTransaction.getType() != TransactionType.USE) {
            throw new PointException(PointErrorCode.NOT_USE_TRANSACTION);
        }

        if (cancelAmount == null || cancelAmount < 1) {
            throw new PointException(PointErrorCode.INVALID_CANCEL_AMOUNT);
        }

        List<UsageDetail> usageDetails = usageDetailRepository
                .findByUseTransactionIdWithLedger(useTransaction.getTransactionId());

        long totalCancellable = usageDetails.stream()
                .mapToLong(UsageDetail::getCancellableAmount)
                .sum();

        if (cancelAmount > totalCancellable) {
            throw new PointException(PointErrorCode.EXCEED_CANCELLABLE_AMOUNT);
        }

        Wallet wallet = walletRepository.findByMemberId(useTransaction.getMemberId())
                .orElseThrow(() -> new PointException(PointErrorCode.WALLET_NOT_FOUND));

        // FIFO 순서로 복원 처리
        long remaining = cancelAmount;
        long totalRestored = 0;

        for (UsageDetail detail : usageDetails) {
            if (remaining <= 0) break;

            long cancellable = detail.getCancellableAmount();
            if (cancellable <= 0) continue;

            long restoreAmount = Math.min(remaining, cancellable);
            detail.addCancelAmount(restoreAmount);

            Ledger ledger = detail.getLedger();

            if (ledger.isActive() && !isExpiredNow(ledger)) {
                // 미만료 → balance 복원
                ledger.restore(restoreAmount);
            } else {
                // 만료됨 → USE_CANCEL_RESTORE 신규 적립건 생성
                int restoreExpireDays = (int) configService.getConfigValue(ConfigKey.USE_CANCEL_RESTORE_EXPIRE_DAYS);
                LocalDate restoreStartDate = LocalDate.now();
                LocalDate restoreExpireDate = restoreStartDate.plusDays(restoreExpireDays);
                Ledger restoreLedger = Ledger.builder()
                        .memberId(useTransaction.getMemberId())
                        .earnAmount(restoreAmount)
                        .earnType(EarnType.USE_CANCEL_RESTORE)
                        .description("사용취소 복원 (원본 적립건: " + ledger.getPointKey() + ")")
                        .sourceTransactionId(useTransaction.getTransactionId())
                        .startAt(restoreStartDate.atStartOfDay())
                        .expireAt(restoreExpireDate.atTime(23, 59, 59))
                        .build();
                ledgerRepository.save(restoreLedger);
            }

            totalRestored += restoreAmount;
            remaining -= restoreAmount;
        }

        wallet.addBalance(totalRestored);

        Transaction cancelTransaction = Transaction.builder()
                .memberId(useTransaction.getMemberId())
                .type(TransactionType.USE_CANCEL)
                .amount(cancelAmount)
                .orderId(useTransaction.getOrderId())
                .relatedPointKey(usePointKey)
                .description("사용취소" + (cancelAmount.equals(useTransaction.getAmount()) ? "(전체)" : "(부분)"))
                .idempotencyKey(idempotencyKey)
                .build();
        transactionRepository.save(cancelTransaction);

        return cancelTransaction;
    }

    private boolean isExpiredNow(Ledger ledger) {
        return ledger.isExpired() || ledger.getExpireAt().isBefore(LocalDateTime.now());
    }
}
