package com.musinsa.point.service;

import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.domain.UsageDetail;
import com.musinsa.point.domain.Wallet;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.TransactionType;
import com.musinsa.point.exception.PointException;
import com.musinsa.point.repository.LedgerRepository;
import com.musinsa.point.repository.TransactionRepository;
import com.musinsa.point.repository.UsageDetailRepository;
import com.musinsa.point.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueryService 단위 테스트")
class QueryServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UsageDetailRepository usageDetailRepository;

    @InjectMocks
    private QueryService queryService;

    @Test
    @DisplayName("잔액 조회 성공")
    void getBalance_success() {
        // given
        Long memberId = 1L;
        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .totalBalance(1000L)
                .build();
        given(walletRepository.findByMemberId(memberId))
                .willReturn(Optional.of(wallet));

        // when
        Wallet result = queryService.getBalance(memberId);

        // then
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getTotalBalance()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("잔액 조회 실패 - 지갑 없음")
    void getBalance_walletNotFound() {
        // given
        Long memberId = 1L;
        given(walletRepository.findByMemberId(memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queryService.getBalance(memberId))
                .isInstanceOf(PointException.class);
    }

    @Test
    @DisplayName("트랜잭션 페이징 조회 성공")
    void getTransactions_success() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .memberId(memberId)
                        .type(TransactionType.EARN)
                        .amount(100L)
                        .build(),
                Transaction.builder()
                        .memberId(memberId)
                        .type(TransactionType.USE)
                        .amount(50L)
                        .build()
        );
        Page<Transaction> page = new PageImpl<>(transactions, pageable, transactions.size());
        given(transactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable))
                .willReturn(page);

        // when
        Page<Transaction> result = queryService.getTransactions(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("원장 페이징 조회 성공")
    void getLedgers_success() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime now = LocalDateTime.now();
        List<Ledger> ledgers = List.of(
                Ledger.builder()
                        .memberId(memberId)
                        .earnAmount(100L)
                        .earnType(EarnType.NORMAL)
                        .description("적립")
                        .startAt(now)
                        .expireAt(now.plusYears(1))
                        .build()
        );
        Page<Ledger> page = new PageImpl<>(ledgers, pageable, ledgers.size());
        given(ledgerRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable))
                .willReturn(page);

        // when
        Page<Ledger> result = queryService.getLedgers(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("주문별 사용 내역 조회 성공")
    void getUsageDetailsByOrderId_success() {
        // given
        String orderId = "ORDER-123";
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .memberId(1L)
                .type(TransactionType.USE)
                .amount(100L)
                .orderId(orderId)
                .build();

        Ledger ledger = Ledger.builder()
                .memberId(1L)
                .earnAmount(100L)
                .earnType(EarnType.NORMAL)
                .description("적립")
                .startAt(now)
                .expireAt(now.plusYears(1))
                .build();

        List<UsageDetail> usageDetails = List.of(
                UsageDetail.builder()
                        .useTransaction(transaction)
                        .ledger(ledger)
                        .amount(50L)
                        .build(),
                UsageDetail.builder()
                        .useTransaction(transaction)
                        .ledger(ledger)
                        .amount(50L)
                        .build()
        );
        given(usageDetailRepository.findByOrderIdWithLedgerAndTransaction(orderId))
                .willReturn(usageDetails);

        // when
        List<UsageDetail> result = queryService.getUsageDetailsByOrderId(orderId);

        // then
        assertThat(result).hasSize(2);
    }
}
