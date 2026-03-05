package com.musinsa.point.service;

import com.musinsa.point.domain.enums.ConfigKey;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.LedgerStatus;
import com.musinsa.point.domain.enums.TransactionType;
import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.domain.Wallet;
import com.musinsa.point.exception.PointErrorCode;
import com.musinsa.point.exception.PointException;
import com.musinsa.point.repository.LedgerRepository;
import com.musinsa.point.repository.TransactionRepository;
import com.musinsa.point.repository.WalletRepository;
import com.musinsa.point.validator.EarnValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EarnService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final ConfigService configService;
    private final EarnValidator earnValidator;
    private final EarnDateTimeResolver dateTimeResolver;

    @Transactional
    public Ledger earn(Long memberId, Long amount, EarnType earnType,
                            String description, String userId,
                            LocalDate startDate, LocalDate expireDate,
                            String idempotencyKey) {
        Long maxEarnAmount = configService.getConfigValue(ConfigKey.MAX_EARN_AMOUNT_PER_ONCE);
        earnValidator.validateEarnAmount(amount, maxEarnAmount);

        startDate = dateTimeResolver.resolveStartDate(startDate);
        int defaultExpireDays = (int) configService.getConfigValue(ConfigKey.DEFAULT_EXPIRE_DAYS);
        expireDate = dateTimeResolver.resolveExpireDate(startDate, expireDate, defaultExpireDays);

        Long minDays = configService.getConfigValue(ConfigKey.MIN_EXPIRE_DAYS);
        Long maxDays = configService.getConfigValue(ConfigKey.MAX_EXPIRE_DAYS);
        earnValidator.validateDateRange(startDate, expireDate, minDays, maxDays);

        Wallet wallet = getOrCreateWallet(memberId);
        Long maxBalance = configService.getConfigValue(ConfigKey.MAX_BALANCE_PER_MEMBER);
        earnValidator.validateMaxBalance(wallet.getTotalBalance(), amount, maxBalance);

        LocalDateTime startAt = dateTimeResolver.calculateStartAt(startDate);
        LocalDateTime expireAt = dateTimeResolver.calculateExpireAt(expireDate);

        Ledger ledger = Ledger.builder()
                .memberId(memberId)
                .earnAmount(amount)
                .earnType(earnType)
                .description(description)
                .userId(userId)
                .startAt(startAt)
                .expireAt(expireAt)
                .build();
        ledgerRepository.save(ledger);

        Transaction transaction = Transaction.builder()
                .memberId(memberId)
                .type(TransactionType.EARN)
                .amount(amount)
                .relatedPointKey(ledger.getPointKey())
                .description(description)
                .idempotencyKey(idempotencyKey)
                .build();
        transactionRepository.save(transaction);

        wallet.addBalance(amount);

        return ledger;
    }

    @Transactional
    public void cancelEarn(String pointKey) {
        Ledger ledger = ledgerRepository.findByPointKey(pointKey)
                .orElseThrow(() -> new PointException(PointErrorCode.LEDGER_NOT_FOUND));

        if (ledger.getStatus() == LedgerStatus.CANCELED) {
            throw new PointException(PointErrorCode.LEDGER_ALREADY_CANCELED);
        }

        if (!ledger.getBalance().equals(ledger.getEarnAmount())) {
            throw new PointException(PointErrorCode.LEDGER_PARTIALLY_USED);
        }

        Wallet wallet = walletRepository.findByMemberId(ledger.getMemberId())
                .orElseThrow(() -> new PointException(PointErrorCode.WALLET_NOT_FOUND));

        wallet.subtractBalance(ledger.getEarnAmount());
        ledger.cancel();

        Transaction transaction = Transaction.builder()
                .memberId(ledger.getMemberId())
                .type(TransactionType.EARN_CANCEL)
                .amount(ledger.getEarnAmount())
                .relatedPointKey(pointKey)
                .description("적립취소: " + ledger.getDescription())
                .build();
        transactionRepository.save(transaction);
    }

    private Wallet getOrCreateWallet(Long memberId) {
        return walletRepository.findByMemberId(memberId)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder().memberId(memberId).totalBalance(0L).build()));
    }
}
