package com.musinsa.point.service;

import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.domain.UsageDetail;
import com.musinsa.point.domain.Wallet;
import com.musinsa.point.exception.PointErrorCode;
import com.musinsa.point.exception.PointException;
import com.musinsa.point.repository.LedgerRepository;
import com.musinsa.point.repository.TransactionRepository;
import com.musinsa.point.repository.UsageDetailRepository;
import com.musinsa.point.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final UsageDetailRepository usageDetailRepository;

    public Wallet getBalance(Long memberId) {
        return walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new PointException(PointErrorCode.WALLET_NOT_FOUND));
    }

    public Page<Transaction> getTransactions(Long memberId, Pageable pageable) {
        return transactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }

    public Page<Ledger> getLedgers(Long memberId, Pageable pageable) {
        return ledgerRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }

    public List<UsageDetail> getUsageDetailsByOrderId(String orderId) {
        return usageDetailRepository.findByOrderIdWithLedgerAndTransaction(orderId);
    }
}
