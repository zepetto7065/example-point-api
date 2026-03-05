package com.musinsa.point.service;

import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.LedgerStatus;
import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Wallet;
import com.musinsa.point.repository.LedgerRepository;
import com.musinsa.point.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExpireService 단위 테스트")
class ExpireServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private ExpireService pointExpireService;

    @Test
    @DisplayName("만료 배치 처리 - 정상 처리 (단일 회원)")
    void expirePoints_success_singleMember() {
        Long memberId = 1L;

        Ledger expiredLedger = Ledger.builder()
                .memberId(memberId)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("적립")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();
        expiredLedger.use(500L);

        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .totalBalance(1500L)
                .build();

        when(ledgerRepository.findExpiredLedgers(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiredLedger));
        when(walletRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(wallet));

        int result = pointExpireService.expirePoints();

        assertThat(result).isEqualTo(1);
        assertThat(expiredLedger.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(expiredLedger.getBalance()).isEqualTo(0L);
        assertThat(wallet.getTotalBalance()).isEqualTo(1000L);

        verify(ledgerRepository).findExpiredLedgers(any(LocalDateTime.class));
        verify(walletRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("만료 배치 처리 - 정상 처리 (복수 회원, 복수 적립건)")
    void expirePoints_success_multipleMembers() {
        Long memberId1 = 1L;
        Long memberId2 = 2L;

        Ledger ledger1 = Ledger.builder()
                .memberId(memberId1)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("회원1 적립1")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();

        Ledger ledger2 = Ledger.builder()
                .memberId(memberId1)
                .earnAmount(500L)
                .earnType(EarnType.NORMAL)
                .description("회원1 적립2")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();

        Ledger ledger3 = Ledger.builder()
                .memberId(memberId2)
                .earnAmount(2000L)
                .earnType(EarnType.NORMAL)
                .description("회원2 적립1")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();

        Wallet wallet1 = Wallet.builder()
                .memberId(memberId1)
                .totalBalance(3000L)
                .build();

        Wallet wallet2 = Wallet.builder()
                .memberId(memberId2)
                .totalBalance(5000L)
                .build();

        when(ledgerRepository.findExpiredLedgers(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(ledger1, ledger2, ledger3));
        when(walletRepository.findByMemberId(memberId1))
                .thenReturn(Optional.of(wallet1));
        when(walletRepository.findByMemberId(memberId2))
                .thenReturn(Optional.of(wallet2));

        int result = pointExpireService.expirePoints();

        assertThat(result).isEqualTo(3);

        assertThat(ledger1.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(ledger2.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(ledger3.getStatus()).isEqualTo(LedgerStatus.EXPIRED);

        assertThat(wallet1.getTotalBalance()).isEqualTo(1500L);
        assertThat(wallet2.getTotalBalance()).isEqualTo(3000L);

        verify(walletRepository, times(2)).findByMemberId(memberId1);
        verify(walletRepository, times(1)).findByMemberId(memberId2);
    }

    @Test
    @DisplayName("만료 배치 처리 - 만료 대상 없음")
    void expirePoints_noExpiredLedgers() {
        when(ledgerRepository.findExpiredLedgers(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        int result = pointExpireService.expirePoints();

        assertThat(result).isEqualTo(0);

        verify(ledgerRepository).findExpiredLedgers(any(LocalDateTime.class));
        verify(walletRepository, never()).findByMemberId(anyLong());
    }

    @Test
    @DisplayName("만료 배치 처리 - 잔액이 0인 적립건은 건너뜀")
    void expirePoints_skipZeroBalance() {
        Long memberId = 1L;

        Ledger ledgerWithZeroBalance = Ledger.builder()
                .memberId(memberId)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("이미 모두 사용됨")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();
        ledgerWithZeroBalance.use(1000L);

        Ledger ledgerWithBalance = Ledger.builder()
                .memberId(memberId)
                .earnAmount(500L)
                .earnType(EarnType.NORMAL)
                .description("일부 사용됨")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();
        ledgerWithBalance.use(200L);

        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .totalBalance(1000L)
                .build();

        when(ledgerRepository.findExpiredLedgers(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(ledgerWithZeroBalance, ledgerWithBalance));
        when(walletRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(wallet));

        int result = pointExpireService.expirePoints();

        assertThat(result).isEqualTo(1);
        assertThat(ledgerWithZeroBalance.getStatus()).isEqualTo(LedgerStatus.ACTIVE);
        assertThat(ledgerWithBalance.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(ledgerWithBalance.getBalance()).isEqualTo(0L);
        assertThat(wallet.getTotalBalance()).isEqualTo(700L);

        verify(walletRepository, times(1)).findByMemberId(memberId);
    }

    @Test
    @DisplayName("만료 배치 처리 - 지갑이 없는 경우 (방어 로직)")
    void expirePoints_walletNotFound() {
        Long memberId = 1L;

        Ledger ledger = Ledger.builder()
                .memberId(memberId)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("적립")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();

        when(ledgerRepository.findExpiredLedgers(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(ledger));
        when(walletRepository.findByMemberId(memberId))
                .thenReturn(Optional.empty());

        int result = pointExpireService.expirePoints();

        assertThat(result).isEqualTo(1);
        assertThat(ledger.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(ledger.getBalance()).isEqualTo(0L);

        verify(walletRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("만료 배치 처리 - 부분 사용된 적립건 만료")
    void expirePoints_partiallyUsedLedger() {
        Long memberId = 1L;

        Ledger ledger = Ledger.builder()
                .memberId(memberId)
                .earnAmount(2000L)
                .earnType(EarnType.NORMAL)
                .description("부분 사용됨")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();
        ledger.use(800L);

        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .totalBalance(3000L)
                .build();

        when(ledgerRepository.findExpiredLedgers(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(ledger));
        when(walletRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(wallet));

        int result = pointExpireService.expirePoints();

        assertThat(result).isEqualTo(1);
        assertThat(ledger.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(ledger.getBalance()).isEqualTo(0L);
        assertThat(wallet.getTotalBalance()).isEqualTo(1800L);

        verify(ledgerRepository).findExpiredLedgers(any(LocalDateTime.class));
        verify(walletRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("만료 배치 처리 - ADMIN_MANUAL 타입 만료")
    void expirePoints_adminManualType() {
        Long memberId = 1L;

        Ledger adminLedger = Ledger.builder()
                .memberId(memberId)
                .earnAmount(5000L)
                .earnType(EarnType.ADMIN_MANUAL)
                .description("관리자 수기 지급")
                .userId("admin123")
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();

        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .totalBalance(10000L)
                .build();

        when(ledgerRepository.findExpiredLedgers(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(adminLedger));
        when(walletRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(wallet));

        int result = pointExpireService.expirePoints();

        assertThat(result).isEqualTo(1);
        assertThat(adminLedger.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(adminLedger.getBalance()).isEqualTo(0L);
        assertThat(wallet.getTotalBalance()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("만료 배치 처리 - USE_CANCEL_RESTORE 타입 만료")
    void expirePoints_useCancelRestoreType() {
        Long memberId = 1L;

        Ledger restoreLedger = Ledger.builder()
                .memberId(memberId)
                .earnAmount(1500L)
                .earnType(EarnType.USE_CANCEL_RESTORE)
                .description("사용취소 복원")
                .userId(null)
                .sourceTransactionId(999L)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();

        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .totalBalance(2000L)
                .build();

        when(ledgerRepository.findExpiredLedgers(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(restoreLedger));
        when(walletRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(wallet));

        int result = pointExpireService.expirePoints();

        assertThat(result).isEqualTo(1);
        assertThat(restoreLedger.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(restoreLedger.getBalance()).isEqualTo(0L);
        assertThat(wallet.getTotalBalance()).isEqualTo(500L);
    }

    @Test
    @DisplayName("만료 배치 처리 - 다양한 잔액의 적립건 혼합")
    void expirePoints_mixedBalances() {
        Long memberId = 1L;

        Ledger fullBalance = Ledger.builder()
                .memberId(memberId)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("전액 잔여")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();

        Ledger partialBalance = Ledger.builder()
                .memberId(memberId)
                .earnAmount(500L)
                .earnType(EarnType.NORMAL)
                .description("일부 잔여")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();
        partialBalance.use(200L);

        Ledger zeroBalance = Ledger.builder()
                .memberId(memberId)
                .earnAmount(800L)
                .earnType(EarnType.NORMAL)
                .description("잔액 없음")
                .userId(null)
                .expireAt(LocalDateTime.now().minusDays(1))
                .build();
        zeroBalance.use(800L);

        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .totalBalance(5000L)
                .build();

        when(ledgerRepository.findExpiredLedgers(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(fullBalance, partialBalance, zeroBalance));
        when(walletRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(wallet));

        int result = pointExpireService.expirePoints();

        assertThat(result).isEqualTo(2);

        assertThat(fullBalance.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(partialBalance.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(zeroBalance.getStatus()).isEqualTo(LedgerStatus.ACTIVE);

        assertThat(wallet.getTotalBalance()).isEqualTo(3700L);

        verify(walletRepository, times(2)).findByMemberId(memberId);
    }
}
