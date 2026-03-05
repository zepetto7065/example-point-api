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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EarnService 단위 테스트")
class EarnServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ConfigService configService;

    @Mock
    private EarnValidator earnValidator;

    @Mock
    private EarnDateTimeResolver dateTimeResolver;

    @InjectMocks
    private EarnService pointEarnService;

    private static final Long MEMBER_ID = 1L;
    private static final Long EARN_AMOUNT = 1000L;
    private static final String DESCRIPTION = "적립 테스트";
    private static final String USER_ID = "admin123";

    @BeforeEach
    void setUp() {
        when(configService.getConfigValue(ConfigKey.MAX_EARN_AMOUNT_PER_ONCE)).thenReturn(10000L);
        when(configService.getConfigValue(ConfigKey.MAX_BALANCE_PER_MEMBER)).thenReturn(100000L);
        when(configService.getConfigValue(ConfigKey.DEFAULT_EXPIRE_DAYS)).thenReturn(365L);
        when(configService.getConfigValue(ConfigKey.MIN_EXPIRE_DAYS)).thenReturn(1L);
        when(configService.getConfigValue(ConfigKey.MAX_EXPIRE_DAYS)).thenReturn(730L);

        // EarnDateTimeResolver stubbing
        when(dateTimeResolver.resolveStartDate(any())).thenAnswer(invocation -> {
            LocalDate input = invocation.getArgument(0);
            return input != null ? input : LocalDate.now();
        });
        when(dateTimeResolver.resolveExpireDate(any(), any(), anyInt())).thenAnswer(invocation -> {
            LocalDate startDate = invocation.getArgument(0);
            LocalDate expireDate = invocation.getArgument(1);
            Integer defaultDays = invocation.getArgument(2);
            return expireDate != null ? expireDate : startDate.plusDays(defaultDays);
        });
        when(dateTimeResolver.calculateStartAt(any())).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(0);
            return date.atStartOfDay();
        });
        when(dateTimeResolver.calculateExpireAt(any())).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(0);
            return date.atTime(23, 59, 59);
        });
    }

    @Test
    @DisplayName("정상 적립 - 신규 지갑 생성")
    void earn_success_newWallet() {
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());
        Wallet newWallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(0L)
                .build();
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);
        when(ledgerRepository.save(any(Ledger.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ledger result = pointEarnService.earn(MEMBER_ID, EARN_AMOUNT, EarnType.NORMAL, DESCRIPTION, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(result.getEarnAmount()).isEqualTo(EARN_AMOUNT);
        assertThat(result.getBalance()).isEqualTo(EARN_AMOUNT);
        assertThat(result.getEarnType()).isEqualTo(EarnType.NORMAL);
        assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(result.getStatus()).isEqualTo(LedgerStatus.ACTIVE);

        verify(walletRepository).findByMemberId(MEMBER_ID);
        verify(walletRepository).save(any(Wallet.class));
        verify(ledgerRepository).save(any(Ledger.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("정상 적립 - 기존 지갑 사용")
    void earn_success_existingWallet() {
        Wallet existingWallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(5000L)
                .build();
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(existingWallet));
        when(ledgerRepository.save(any(Ledger.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ledger result = pointEarnService.earn(MEMBER_ID, EARN_AMOUNT, EarnType.NORMAL, DESCRIPTION, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(existingWallet.getTotalBalance()).isEqualTo(6000L);

        verify(walletRepository).findByMemberId(MEMBER_ID);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(ledgerRepository).save(any(Ledger.class));
    }

    @Test
    @DisplayName("적립 실패 - 1회 최대 적립 금액 초과")
    void earn_fail_exceedMaxEarnAmount() {
        Long excessAmount = 15000L;
        when(configService.getConfigValue(ConfigKey.MAX_EARN_AMOUNT_PER_ONCE)).thenReturn(10000L);
        doThrow(new PointException(PointErrorCode.EXCEED_MAX_EARN_AMOUNT))
                .when(earnValidator).validateEarnAmount(anyLong(), anyLong());

        assertThatThrownBy(() ->
                pointEarnService.earn(MEMBER_ID, excessAmount, EarnType.NORMAL, DESCRIPTION, null, null, null, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.EXCEED_MAX_EARN_AMOUNT);

        verify(walletRepository, never()).findByMemberId(anyLong());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("적립 실패 - 최대 보유 한도 초과")
    void earn_fail_exceedMaxBalance() {
        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(95000L)
                .build();
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));

        Long earnAmount = 6000L;
        when(configService.getConfigValue(ConfigKey.MAX_EARN_AMOUNT_PER_ONCE)).thenReturn(10000L);
        when(configService.getConfigValue(ConfigKey.MIN_EXPIRE_DAYS)).thenReturn(1L);
        when(configService.getConfigValue(ConfigKey.MAX_EXPIRE_DAYS)).thenReturn(730L);
        when(configService.getConfigValue(ConfigKey.MAX_BALANCE_PER_MEMBER)).thenReturn(100000L);
        doThrow(new PointException(PointErrorCode.EXCEED_MAX_BALANCE))
                .when(earnValidator).validateMaxBalance(anyLong(), anyLong(), anyLong());

        assertThatThrownBy(() ->
                pointEarnService.earn(MEMBER_ID, earnAmount, EarnType.NORMAL, DESCRIPTION, null, null, null, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.EXCEED_MAX_BALANCE);

        verify(walletRepository).findByMemberId(MEMBER_ID);
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("적립 실패 - 만료일이 시작일 이전")
    void earn_fail_expireDateBeforeStartDate() {
        LocalDate startDate = LocalDate.now();
        LocalDate expireDate = startDate.minusDays(1);

        when(configService.getConfigValue(ConfigKey.MAX_EARN_AMOUNT_PER_ONCE)).thenReturn(10000L);
        when(configService.getConfigValue(ConfigKey.MIN_EXPIRE_DAYS)).thenReturn(1L);
        when(configService.getConfigValue(ConfigKey.MAX_EXPIRE_DAYS)).thenReturn(730L);
        doThrow(new PointException(PointErrorCode.INVALID_EXPIRE_DATE))
                .when(earnValidator).validateDateRange(any(LocalDate.class), any(LocalDate.class), anyLong(), anyLong());

        assertThatThrownBy(() ->
                pointEarnService.earn(MEMBER_ID, EARN_AMOUNT, EarnType.NORMAL, DESCRIPTION, null, startDate, expireDate, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EXPIRE_DATE);

        verify(walletRepository, never()).findByMemberId(anyLong());
    }

    @Test
    @DisplayName("적립 실패 - 만료일 범위 벗어남 (최대값 초과)")
    void earn_fail_expireDaysAboveMax() {
        LocalDate startDate = LocalDate.now();
        LocalDate expireDate = startDate.plusDays(1000);

        when(configService.getConfigValue(ConfigKey.MAX_EARN_AMOUNT_PER_ONCE)).thenReturn(10000L);
        when(configService.getConfigValue(ConfigKey.MIN_EXPIRE_DAYS)).thenReturn(1L);
        when(configService.getConfigValue(ConfigKey.MAX_EXPIRE_DAYS)).thenReturn(730L);
        doThrow(new PointException(PointErrorCode.INVALID_EXPIRE_DAYS))
                .when(earnValidator).validateDateRange(any(LocalDate.class), any(LocalDate.class), anyLong(), anyLong());

        assertThatThrownBy(() ->
                pointEarnService.earn(MEMBER_ID, EARN_AMOUNT, EarnType.NORMAL, DESCRIPTION, null, startDate, expireDate, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EXPIRE_DAYS);

        verify(walletRepository, never()).findByMemberId(anyLong());
    }

    @Test
    @DisplayName("관리자 수기 지급 성공")
    void earn_success_adminManual() {
        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(0L)
                .build();
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(ledgerRepository.save(any(Ledger.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate startDate = LocalDate.now();
        LocalDate expireDate = startDate.plusDays(30);
        when(configService.getConfigValue(ConfigKey.MIN_EXPIRE_DAYS)).thenReturn(1L);
        when(configService.getConfigValue(ConfigKey.MAX_EXPIRE_DAYS)).thenReturn(730L);

        Ledger result = pointEarnService.earn(MEMBER_ID, EARN_AMOUNT, EarnType.ADMIN_MANUAL,
                "관리자 수기 지급", USER_ID, startDate, expireDate, null);

        assertThat(result).isNotNull();
        assertThat(result.getEarnType()).isEqualTo(EarnType.ADMIN_MANUAL);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getDescription()).isEqualTo("관리자 수기 지급");

        ArgumentCaptor<Ledger> ledgerCaptor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepository).save(ledgerCaptor.capture());

        Ledger savedLedger = ledgerCaptor.getValue();
        assertThat(savedLedger.getStartAt()).isEqualTo(startDate.atStartOfDay());
        assertThat(savedLedger.getExpireAt()).isEqualTo(expireDate.atTime(23, 59, 59));
    }

    @Test
    @DisplayName("적립 취소 성공")
    void cancelEarn_success() {
        String pointKey = "test-point-key-123";
        Ledger ledger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(EARN_AMOUNT)
                .earnType(EarnType.NORMAL)
                .description(DESCRIPTION)
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();

        Wallet wallet = Wallet.builder()
                .memberId(MEMBER_ID)
                .totalBalance(5000L)
                .build();

        when(ledgerRepository.findByPointKey(pointKey)).thenReturn(Optional.of(ledger));
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pointEarnService.cancelEarn(pointKey);

        assertThat(ledger.getStatus()).isEqualTo(LedgerStatus.CANCELED);
        assertThat(ledger.getBalance()).isEqualTo(0L);
        assertThat(wallet.getTotalBalance()).isEqualTo(4000L);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getType()).isEqualTo(TransactionType.EARN_CANCEL);
        assertThat(savedTransaction.getAmount()).isEqualTo(EARN_AMOUNT);
        assertThat(savedTransaction.getRelatedPointKey()).isEqualTo(pointKey);
        assertThat(savedTransaction.getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("적립 취소 실패 - 이미 취소됨")
    void cancelEarn_fail_alreadyCanceled() {
        String pointKey = "test-point-key-123";
        Ledger ledger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(EARN_AMOUNT)
                .earnType(EarnType.NORMAL)
                .description(DESCRIPTION)
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();
        ledger.cancel();

        when(ledgerRepository.findByPointKey(pointKey)).thenReturn(Optional.of(ledger));

        assertThatThrownBy(() -> pointEarnService.cancelEarn(pointKey))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.LEDGER_ALREADY_CANCELED);

        verify(walletRepository, never()).findByMemberId(anyLong());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("적립 취소 실패 - 일부 사용됨")
    void cancelEarn_fail_partiallyUsed() {
        String pointKey = "test-point-key-123";
        Ledger ledger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(EARN_AMOUNT)
                .earnType(EarnType.NORMAL)
                .description(DESCRIPTION)
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();
        ledger.use(300L);

        when(ledgerRepository.findByPointKey(pointKey)).thenReturn(Optional.of(ledger));

        assertThatThrownBy(() -> pointEarnService.cancelEarn(pointKey))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.LEDGER_PARTIALLY_USED);

        verify(walletRepository, never()).findByMemberId(anyLong());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("적립 실패 - 금액이 null")
    void earn_fail_amountIsNull() {
        when(configService.getConfigValue(ConfigKey.MAX_EARN_AMOUNT_PER_ONCE)).thenReturn(10000L);
        doThrow(new PointException(PointErrorCode.INVALID_EARN_AMOUNT))
                .when(earnValidator).validateEarnAmount(any(), anyLong());

        assertThatThrownBy(() ->
                pointEarnService.earn(MEMBER_ID, null, EarnType.NORMAL, DESCRIPTION, null, null, null, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EARN_AMOUNT);
    }

    @Test
    @DisplayName("적립 실패 - 금액이 0 이하")
    void earn_fail_amountIsZeroOrNegative() {
        when(configService.getConfigValue(ConfigKey.MAX_EARN_AMOUNT_PER_ONCE)).thenReturn(10000L);
        doThrow(new PointException(PointErrorCode.INVALID_EARN_AMOUNT))
                .when(earnValidator).validateEarnAmount(anyLong(), anyLong());

        assertThatThrownBy(() ->
                pointEarnService.earn(MEMBER_ID, 0L, EarnType.NORMAL, DESCRIPTION, null, null, null, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EARN_AMOUNT);

        assertThatThrownBy(() ->
                pointEarnService.earn(MEMBER_ID, -100L, EarnType.NORMAL, DESCRIPTION, null, null, null, null))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.INVALID_EARN_AMOUNT);
    }

    @Test
    @DisplayName("적립 취소 실패 - 적립건 없음")
    void cancelEarn_fail_ledgerNotFound() {
        String pointKey = "non-existent-key";
        when(ledgerRepository.findByPointKey(pointKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointEarnService.cancelEarn(pointKey))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.LEDGER_NOT_FOUND);
    }

    @Test
    @DisplayName("적립 취소 실패 - 지갑 없음")
    void cancelEarn_fail_walletNotFound() {
        String pointKey = "test-point-key-123";
        Ledger ledger = Ledger.builder()
                .memberId(MEMBER_ID)
                .earnAmount(EARN_AMOUNT)
                .earnType(EarnType.NORMAL)
                .description(DESCRIPTION)
                .userId(null)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();

        when(ledgerRepository.findByPointKey(pointKey)).thenReturn(Optional.of(ledger));
        when(walletRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointEarnService.cancelEarn(pointKey))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", PointErrorCode.WALLET_NOT_FOUND);
    }
}
