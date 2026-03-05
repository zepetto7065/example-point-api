package com.musinsa.point.service;

import com.musinsa.point.domain.enums.ConfigKey;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.LedgerStatus;
import com.musinsa.point.domain.enums.TransactionType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UseService 단위 테스트")
class UseServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UsageDetailRepository usageDetailRepository;

    @Mock
    private ConfigService configService;

    @InjectMocks
    private UseService pointUseService;

    private static final Long MEMBER_ID = 1L;
    private static final String ORDER_ID = "ORDER-123";
    private static final String DESCRIPTION = "상품 구매";

    @BeforeEach
    void setUp() {
        when(configService.getConfigValue(ConfigKey.USE_CANCEL_RESTORE_EXPIRE_DAYS)).thenReturn(30L);
    }

    @Test
    @DisplayName("정상 사용 - 단일 적립건에서 차감")
    void use_success_singleLedger() {
        Long useAmount = 500L;
        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(1000L)
                .build();

        Ledger ledger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("적립")
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();

        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(ledgerRepository.findDeductibleLedgers(eq(MEMBER_ID), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(ledger));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = pointUseService.use(MEMBER_ID, useAmount, ORDER_ID, DESCRIPTION, null);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.USE);
        assertThat(result.getAmount()).isEqualTo(useAmount);
        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(ledger.getBalance()).isEqualTo(500L);
        assertThat(wallet.getTotalBalance()).isEqualTo(500L);

        verify(usageDetailRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("정상 사용 - 복수 적립건에서 차감")
    void use_success_multipleLedgers() {
        Long useAmount = 1500L;
        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(2000L)
                .build();

        Ledger ledger1 = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("첫 번째 적립")
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(30).atTime(23, 59, 59))
                .build();

        Ledger ledger2 = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("두 번째 적립")
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(60).atTime(23, 59, 59))
                .build();

        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(ledgerRepository.findDeductibleLedgers(eq(MEMBER_ID), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(ledger1, ledger2));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = pointUseService.use(MEMBER_ID, useAmount, ORDER_ID, DESCRIPTION, null);

        assertThat(result).isNotNull();
        assertThat(ledger1.getBalance()).isEqualTo(0L);
        assertThat(ledger2.getBalance()).isEqualTo(500L);
        assertThat(wallet.getTotalBalance()).isEqualTo(500L);

        ArgumentCaptor<List<UsageDetail>> detailsCaptor = ArgumentCaptor.forClass(List.class);
        verify(usageDetailRepository).saveAll(detailsCaptor.capture());
        List<UsageDetail> savedDetails = detailsCaptor.getValue();
        assertThat(savedDetails).hasSize(2);
        assertThat(savedDetails.get(0).getAmount()).isEqualTo(1000L);
        assertThat(savedDetails.get(1).getAmount()).isEqualTo(500L);
    }

    @Test
    @DisplayName("사용 실패 - 잔액 부족 (지갑)")
    void use_fail_insufficientBalance_wallet() {
        Long useAmount = 2000L;
        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(1000L)
                .build();

        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> pointUseService.use(MEMBER_ID, useAmount, ORDER_ID, DESCRIPTION, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INSUFFICIENT_BALANCE);

        verify(ledgerRepository, never()).findDeductibleLedgers(anyLong(), any());
    }

    @Test
    @DisplayName("사용 실패 - 잔액 부족 (차감 가능 적립건)")
    void use_fail_insufficientBalance_ledgers() {
        Long useAmount = 1500L;
        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(1500L)
                .build();

        Ledger ledger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("적립")
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();

        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(ledgerRepository.findDeductibleLedgers(eq(MEMBER_ID), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(ledger));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> pointUseService.use(MEMBER_ID, useAmount, ORDER_ID, DESCRIPTION, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("차감 순서 검증 - ADMIN_MANUAL 우선")
    void use_deductionOrder_adminManualFirst() {
        Long useAmount = 1500L;
        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(3000L)
                .build();

        Ledger adminLedger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(1000L)
                .earnType(EarnType.ADMIN_MANUAL)
                .description("관리자 지급")
                .userId("admin")
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();

        Ledger normalLedger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(2000L)
                .earnType(EarnType.NORMAL)
                .description("일반 적립")
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(30).atTime(23, 59, 59))
                .build();

        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(ledgerRepository.findDeductibleLedgers(eq(MEMBER_ID), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(adminLedger, normalLedger));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pointUseService.use(MEMBER_ID, useAmount, ORDER_ID, DESCRIPTION, null);

        assertThat(adminLedger.getBalance()).isEqualTo(0L);
        assertThat(normalLedger.getBalance()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("사용 취소 - 전체 취소 (미만료 적립건)")
    void cancelUse_success_fullCancel_notExpired() {
        String usePointKey = "use-point-key-123";
        Long useAmount = 1000L;
        Long cancelAmount = 1000L;

        Transaction useTransaction = Transaction.builder()
                .memberId(MEMBER_ID)
                .type(TransactionType.USE)
                .amount(useAmount)
                .orderId(ORDER_ID)
                .description(DESCRIPTION)
                .build();

        Ledger ledger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("적립")
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();
        ledger.use(1000L);

        UsageDetail usageDetail = UsageDetail.builder()
                .useTransaction(useTransaction)
                .ledger(ledger)
                .amount(1000L)
                .build();

        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(0L)
                .build();

        when(transactionRepository.findByPointKey(usePointKey)).thenReturn(Optional.of(useTransaction));
        when(usageDetailRepository.findByUseTransactionIdWithLedger(any()))
                .thenReturn(Collections.singletonList(usageDetail));
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = pointUseService.cancelUse(usePointKey, cancelAmount, null);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.USE_CANCEL);
        assertThat(result.getAmount()).isEqualTo(cancelAmount);
        assertThat(result.getRelatedPointKey()).isEqualTo(usePointKey);
        assertThat(result.getDescription()).contains("전체");

        assertThat(ledger.getBalance()).isEqualTo(1000L);
        assertThat(wallet.getTotalBalance()).isEqualTo(1000L);
        assertThat(usageDetail.getCancelAmount()).isEqualTo(1000L);

        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용 취소 - 부분 취소")
    void cancelUse_success_partialCancel() {
        String usePointKey = "use-point-key-123";
        Long useAmount = 1000L;
        Long cancelAmount = 600L;

        Transaction useTransaction = Transaction.builder()
                .memberId(MEMBER_ID)
                .type(TransactionType.USE)
                .amount(useAmount)
                .orderId(ORDER_ID)
                .description(DESCRIPTION)
                .build();

        Ledger ledger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("적립")
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();
        ledger.use(1000L);

        UsageDetail usageDetail = UsageDetail.builder()
                .useTransaction(useTransaction)
                .ledger(ledger)
                .amount(1000L)
                .build();

        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(0L)
                .build();

        when(transactionRepository.findByPointKey(usePointKey)).thenReturn(Optional.of(useTransaction));
        when(usageDetailRepository.findByUseTransactionIdWithLedger(any()))
                .thenReturn(Collections.singletonList(usageDetail));
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = pointUseService.cancelUse(usePointKey, cancelAmount, null);

        assertThat(result).isNotNull();
        assertThat(result.getDescription()).contains("부분");
        assertThat(ledger.getBalance()).isEqualTo(600L);
        assertThat(wallet.getTotalBalance()).isEqualTo(600L);
        assertThat(usageDetail.getCancelAmount()).isEqualTo(600L);
        assertThat(usageDetail.getCancellableAmount()).isEqualTo(400L);
    }

    @Test
    @DisplayName("사용 취소 - 만료된 적립건은 신규 적립건 생성")
    void cancelUse_expiredLedger_createNewRestoreLedger() {
        String usePointKey = "use-point-key-123";
        Long useAmount = 1000L;
        Long cancelAmount = 1000L;

        Transaction useTransaction = Transaction.builder()
                .memberId(MEMBER_ID)
                .type(TransactionType.USE)
                .amount(useAmount)
                .orderId(ORDER_ID)
                .description(DESCRIPTION)
                .build();

        Ledger expiredLedger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("적립")
                .userId(null)
                .startAt(LocalDate.now().minusDays(31).atStartOfDay())
                .expireAt(LocalDate.now().minusDays(1).atTime(23, 59, 59))
                .build();
        expiredLedger.expire();

        UsageDetail usageDetail = UsageDetail.builder()
                .useTransaction(useTransaction)
                .ledger(expiredLedger)
                .amount(1000L)
                .build();

        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(0L)
                .build();

        when(transactionRepository.findByPointKey(usePointKey)).thenReturn(Optional.of(useTransaction));
        when(usageDetailRepository.findByUseTransactionIdWithLedger(any()))
                .thenReturn(Collections.singletonList(usageDetail));
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(configService.getConfigValue(ConfigKey.USE_CANCEL_RESTORE_EXPIRE_DAYS)).thenReturn(30L);

        Transaction result = pointUseService.cancelUse(usePointKey, cancelAmount, null);

        assertThat(result).isNotNull();
        assertThat(expiredLedger.getBalance()).isEqualTo(0L);
        assertThat(wallet.getTotalBalance()).isEqualTo(1000L);

        ArgumentCaptor<Ledger> ledgerCaptor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepository).save(ledgerCaptor.capture());

        Ledger restoredLedger = ledgerCaptor.getValue();
        assertThat(restoredLedger.getEarnType()).isEqualTo(EarnType.USE_CANCEL_RESTORE);
        assertThat(restoredLedger.getEarnAmount()).isEqualTo(1000L);
        assertThat(restoredLedger.getBalance()).isEqualTo(1000L);
        assertThat(restoredLedger.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(restoredLedger.getDescription()).contains("사용취소 복원");
        assertThat(restoredLedger.getStatus()).isEqualTo(LedgerStatus.ACTIVE);
    }

    @Test
    @DisplayName("사용 실패 - 금액이 null 또는 0 이하")
    void use_fail_invalidAmount() {
        assertThatThrownBy(() -> pointUseService.use(MEMBER_ID, null, ORDER_ID, DESCRIPTION, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_USE_AMOUNT);

        assertThatThrownBy(() -> pointUseService.use(MEMBER_ID, 0L, ORDER_ID, DESCRIPTION, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_USE_AMOUNT);

        assertThatThrownBy(() -> pointUseService.use(MEMBER_ID, -100L, ORDER_ID, DESCRIPTION, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_USE_AMOUNT);
    }

    @Test
    @DisplayName("사용 실패 - 지갑 없음")
    void use_fail_walletNotFound() {
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointUseService.use(MEMBER_ID, 1000L, ORDER_ID, DESCRIPTION, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.WALLET_NOT_FOUND);
    }

    @Test
    @DisplayName("사용 취소 실패 - 거래 없음")
    void cancelUse_fail_transactionNotFound() {
        String usePointKey = "non-existent-key";
        when(transactionRepository.findByPointKey(usePointKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointUseService.cancelUse(usePointKey, 1000L, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.TRANSACTION_NOT_FOUND);
    }

    @Test
    @DisplayName("사용 취소 실패 - 사용 거래가 아님")
    void cancelUse_fail_notUseTransaction() {
        String pointKey = "earn-point-key-123";
        Transaction earnTransaction = Transaction.builder()
                .memberId(MEMBER_ID)
                .type(TransactionType.EARN)
                .amount(1000L)
                .orderId(null)
                .description("적립")
                .build();

        when(transactionRepository.findByPointKey(pointKey)).thenReturn(Optional.of(earnTransaction));

        assertThatThrownBy(() -> pointUseService.cancelUse(pointKey, 1000L, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.NOT_USE_TRANSACTION);
    }

    @Test
    @DisplayName("사용 취소 실패 - 취소 금액 유효하지 않음")
    void cancelUse_fail_invalidCancelAmount() {
        String usePointKey = "use-point-key-123";
        Transaction useTransaction = Transaction.builder()
                .memberId(MEMBER_ID)
                .type(TransactionType.USE)
                .amount(1000L)
                .orderId(ORDER_ID)
                .description(DESCRIPTION)
                .build();

        when(transactionRepository.findByPointKey(usePointKey)).thenReturn(Optional.of(useTransaction));

        assertThatThrownBy(() -> pointUseService.cancelUse(usePointKey, null, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_CANCEL_AMOUNT);

        assertThatThrownBy(() -> pointUseService.cancelUse(usePointKey, 0L, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_CANCEL_AMOUNT);
    }

    @Test
    @DisplayName("사용 취소 실패 - 취소 가능 금액 초과")
    void cancelUse_fail_exceedCancellableAmount() {
        String usePointKey = "use-point-key-123";
        Long useAmount = 1000L;
        Long cancelAmount = 1500L;

        Transaction useTransaction = Transaction.builder()
                .memberId(MEMBER_ID)
                .type(TransactionType.USE)
                .amount(useAmount)
                .orderId(ORDER_ID)
                .description(DESCRIPTION)
                .build();

        Ledger ledger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("적립")
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();

        UsageDetail usageDetail = UsageDetail.builder()
                .useTransaction(useTransaction)
                .ledger(ledger)
                .amount(1000L)
                .build();

        when(transactionRepository.findByPointKey(usePointKey)).thenReturn(Optional.of(useTransaction));
        when(usageDetailRepository.findByUseTransactionIdWithLedger(any()))
                .thenReturn(Collections.singletonList(usageDetail));

        assertThatThrownBy(() -> pointUseService.cancelUse(usePointKey, cancelAmount, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.EXCEED_CANCELLABLE_AMOUNT);
    }
}
