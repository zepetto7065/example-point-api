package com.musinsa.point.batch.job;

import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.LedgerStatus;
import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Wallet;
import com.musinsa.point.repository.LedgerRepository;
import com.musinsa.point.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest
class ExpireJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job pointExpireJob;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private WalletRepository walletRepository;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(pointExpireJob);
        ledgerRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("만료 배치 Job 실행 - 만료 대상 처리")
    void pointExpireJob_success() throws Exception {
        // Given: 만료된 적립건과 지갑 생성
        Long memberId = 1L;

        Ledger expiredLedger = Ledger.builder()
                .memberId(memberId)
                .earnAmount(1000L)
                .earnType(EarnType.NORMAL)
                .description("만료 대상 적립")
                .startAt(LocalDate.now().minusDays(31).atStartOfDay())
                .expireAt(LocalDate.now().minusDays(1).atTime(23, 59, 59))
                .build();
        ledgerRepository.save(expiredLedger);

        Ledger activeLedger = Ledger.builder()
                .memberId(memberId)
                .earnAmount(2000L)
                .earnType(EarnType.NORMAL)
                .description("미만료 적립")
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();
        ledgerRepository.save(activeLedger);

        // When: 배치 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // Then: Job 성공
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 만료된 적립건 검증
        Ledger updatedExpired = ledgerRepository.findById(expiredLedger.getLedgerId()).orElseThrow();
        assertThat(updatedExpired.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(updatedExpired.getBalance()).isEqualTo(0L);

        // 미만료 적립건은 변경 없음
        Ledger updatedActive = ledgerRepository.findById(activeLedger.getLedgerId()).orElseThrow();
        assertThat(updatedActive.getStatus()).isEqualTo(LedgerStatus.ACTIVE);
        assertThat(updatedActive.getBalance()).isEqualTo(2000L);

        // 지갑 잔액 검증: 3000 - 1000 = 2000
        Wallet updatedWallet = walletRepository.findByMemberId(memberId).orElseThrow();
        assertThat(updatedWallet.getTotalBalance()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("만료 배치 Job 실행 - 만료 대상 없음")
    void pointExpireJob_noExpiredLedgers() throws Exception {
        // Given: 만료 대상 없는 적립건만 존재
        Long memberId = 2L;

        walletRepository.save(
                Wallet.builder()
                        .memberId(memberId)
                        .totalBalance(5000L)
                        .build()
        );

        ledgerRepository.save(Ledger.builder()
                .memberId(memberId)
                .earnAmount(5000L)
                .earnType(EarnType.NORMAL)
                .description("미만료 적립")
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build());

        // When: 배치 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // Then: Job 성공, 잔액 변화 없음
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Wallet wallet = walletRepository.findByMemberId(memberId).orElseThrow();
        assertThat(wallet.getTotalBalance()).isEqualTo(5000L);
    }
}
