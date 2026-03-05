package com.musinsa.point.service;

import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.domain.Wallet;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.LedgerStatus;
import com.musinsa.point.domain.enums.TransactionType;
import com.musinsa.point.repository.LedgerRepository;
import com.musinsa.point.repository.TransactionRepository;
import com.musinsa.point.repository.UsageDetailRepository;
import com.musinsa.point.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시나리오:
 * 1. 1000원 적립 (pointKey: A)
 * 2. 500원 적립 (pointKey: B)
 * 3. 주문번호 A1234로 1200원 사용 (pointKey: C)
 *    - A에서 1000원 사용
 *    - B에서 200원 사용
 * 4. A 적립이 만료됨
 * 5. C의 1200원 중 1100원 부분 취소 (pointKey: D)
 *    - A는 만료되었으므로 새로운 pointKey E로 1000원 신규 적립
 *    - B는 만료되지 않았으므로 잔액 복구 (300 → 400)
 * 6. C는 이제 100원만 부분 취소 가능
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.cache.type=none",
    "spring.jpa.defer-datasource-initialization=true"
})
@Transactional
@DisplayName("부분 사용취소 복잡 시나리오 테스트")
class PartialUseCancelScenarioTest {

    @Autowired
    private EarnService earnService;

    @Autowired
    private UseService useService;

    @Autowired
    private ExpireService expireService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UsageDetailRepository usageDetailRepository;

    private static final Long MEMBER_ID = 1L;
    private static final String ORDER_ID = "A1234";

    @Test
    @DisplayName("복잡한 사용취소 시나리오 - 만료된 적립에서 사용한 금액 취소 시 신규 적립")
    void complexPartialUseCancelScenario() {
        // ========================================
        // Step 1: 1000원 적립 (pointKey: A)
        // ========================================
        LocalDate today = LocalDate.now();
        LocalDate expireDate = today.plusDays(30);

        Ledger ledgerA = earnService.earn(
            MEMBER_ID,
            1000L,
            EarnType.NORMAL,
            "첫번째 적립",
            null,
            today,
            expireDate,
            null
        );
        String pointKeyA = ledgerA.getPointKey();

        System.out.println("\n=== Step 1: 1000원 적립 ===");
        System.out.println("pointKey A: " + pointKeyA);

        Wallet wallet = walletRepository.findByMemberId(MEMBER_ID).orElseThrow();
        assertThat(wallet.getTotalBalance()).isEqualTo(1000L);
        assertThat(ledgerA.getBalance()).isEqualTo(1000L);

        // ========================================
        // Step 2: 500원 적립 (pointKey: B)
        // ========================================
        Ledger ledgerB = earnService.earn(
            MEMBER_ID,
            500L,
            EarnType.NORMAL,
            "두번째 적립",
            null,
            today,
            expireDate,
            null
        );
        String pointKeyB = ledgerB.getPointKey();

        System.out.println("\n=== Step 2: 500원 적립 ===");
        System.out.println("pointKey B: " + pointKeyB);

        wallet = walletRepository.findByMemberId(MEMBER_ID).orElseThrow();
        assertThat(wallet.getTotalBalance()).isEqualTo(1500L);
        assertThat(ledgerB.getBalance()).isEqualTo(500L);

        // ========================================
        // Step 3: 주문번호 A1234로 1200원 사용 (pointKey: C)
        // ========================================
        Transaction useTransaction = useService.use(MEMBER_ID, 1200L, ORDER_ID, "주문 A1234 결제", null);
        String pointKeyC = useTransaction.getPointKey();

        System.out.println("\n=== Step 3: 1200원 사용 ===");
        System.out.println("pointKey C: " + pointKeyC);

        wallet = walletRepository.findByMemberId(MEMBER_ID).orElseThrow();
        assertThat(wallet.getTotalBalance()).isEqualTo(300L);

        // A에서 1000원 사용됨
        ledgerA = ledgerRepository.findByPointKey(pointKeyA).orElseThrow();
        assertThat(ledgerA.getBalance()).isEqualTo(0L);
        System.out.println("Ledger A 잔액: 1000 → " + ledgerA.getBalance());

        // B에서 200원 사용됨
        ledgerB = ledgerRepository.findByPointKey(pointKeyB).orElseThrow();
        assertThat(ledgerB.getBalance()).isEqualTo(300L);
        System.out.println("Ledger B 잔액: 500 → " + ledgerB.getBalance());

        // ========================================
        // Step 4: A 적립이 만료됨
        // ========================================
        System.out.println("\n=== Step 4: A 적립 만료 처리 ===");

        // A의 만료일을 과거로 변경
        ledgerA = ledgerRepository.findByPointKey(pointKeyA).orElseThrow();
        ledgerA.expire();
        ledgerRepository.save(ledgerA);

        ledgerA = ledgerRepository.findByPointKey(pointKeyA).orElseThrow();
        assertThat(ledgerA.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        System.out.println("Ledger A 상태: ACTIVE → EXPIRED");

        // ========================================
        // Step 5: C의 1200원 중 1100원 부분 취소 (pointKey: D)
        // ========================================
        System.out.println("\n=== Step 5: 1100원 부분 취소 ===");

        List<Ledger> beforeLedgers = ledgerRepository.findAll();
        System.out.println("취소 전 Ledger 개수: " + beforeLedgers.size());

        Transaction cancelTransaction = useService.cancelUse(pointKeyC, 1100L, null);
        String pointKeyD = cancelTransaction.getPointKey();

        System.out.println("pointKey D: " + pointKeyD);

        wallet = walletRepository.findByMemberId(MEMBER_ID).orElseThrow();
        assertThat(wallet.getTotalBalance()).isEqualTo(1400L);
        System.out.println("총 잔액: 300 → " + wallet.getTotalBalance());

        // A는 만료되었으므로 새로운 pointKey E로 1000원 신규 적립되어야 함
        List<Ledger> allLedgers = ledgerRepository.findAll();
        System.out.println("취소 후 Ledger 개수: " + allLedgers.size());

        List<Ledger> newLedgers = allLedgers.stream()
            .filter(l -> !l.getPointKey().equals(pointKeyA) && !l.getPointKey().equals(pointKeyB))
            .toList();

        assertThat(newLedgers).hasSize(1);
        Ledger ledgerE = newLedgers.get(0);
        String pointKeyE = ledgerE.getPointKey();

        System.out.println("새로운 적립 생성됨!");
        System.out.println("pointKey E: " + pointKeyE);
        System.out.println("Ledger E 금액: " + ledgerE.getEarnAmount());
        System.out.println("Ledger E 잔액: " + ledgerE.getBalance());

        assertThat(ledgerE.getEarnAmount()).isEqualTo(1000L);
        assertThat(ledgerE.getBalance()).isEqualTo(1000L);
        assertThat(ledgerE.getDescription()).contains("사용취소 복원");

        // B는 만료되지 않았으므로 잔액 복구
        ledgerB = ledgerRepository.findByPointKey(pointKeyB).orElseThrow();
        assertThat(ledgerB.getBalance()).isEqualTo(400L);
        System.out.println("Ledger B 잔액: 300 → " + ledgerB.getBalance());

        // A는 여전히 만료 상태
        ledgerA = ledgerRepository.findByPointKey(pointKeyA).orElseThrow();
        assertThat(ledgerA.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(ledgerA.getBalance()).isEqualTo(0L);

        // ========================================
        // Step 6: C는 이제 100원만 부분 취소 가능
        // ========================================
        System.out.println("\n=== Step 6: 남은 100원 취소 가능 여부 확인 ===");

        // 사용 거래 조회
        Transaction useTransactionRefresh = transactionRepository.findByPointKey(pointKeyC).orElseThrow();
        assertThat(useTransactionRefresh.getAmount()).isEqualTo(1200L);

        // 취소된 금액 확인
        Long totalCanceled = transactionRepository.findAll().stream()
            .filter(t -> t.getType() == TransactionType.USE_CANCEL)
            .filter(t -> pointKeyC.equals(t.getRelatedPointKey()))
            .mapToLong(Transaction::getAmount)
            .sum();

        assertThat(totalCanceled).isEqualTo(1100L);
        System.out.println("사용 금액: 1200원");
        System.out.println("취소된 금액: " + totalCanceled + "원");
        System.out.println("취소 가능 금액: " + (1200L - totalCanceled) + "원");

        // 100원 추가 취소
        Transaction finalCancel = useService.cancelUse(pointKeyC, 100L, null);

        wallet = walletRepository.findByMemberId(MEMBER_ID).orElseThrow();
        assertThat(wallet.getTotalBalance()).isEqualTo(1500L);
        System.out.println("최종 총 잔액: " + wallet.getTotalBalance());

        // ========================================
        // 최종 검증
        // ========================================
        System.out.println("\n=== 최종 상태 ===");

        // Ledger A: 만료됨, 잔액 0
        ledgerA = ledgerRepository.findByPointKey(pointKeyA).orElseThrow();
        System.out.println("Ledger A (만료): 잔액 " + ledgerA.getBalance() + ", 상태 " + ledgerA.getStatus());
        assertThat(ledgerA.getStatus()).isEqualTo(LedgerStatus.EXPIRED);
        assertThat(ledgerA.getBalance()).isEqualTo(0L);

        // Ledger B: 활성, 잔액 500 (원래대로 복구)
        ledgerB = ledgerRepository.findByPointKey(pointKeyB).orElseThrow();
        System.out.println("Ledger B (활성): 잔액 " + ledgerB.getBalance() + ", 상태 " + ledgerB.getStatus());
        assertThat(ledgerB.getStatus()).isEqualTo(LedgerStatus.ACTIVE);
        assertThat(ledgerB.getBalance()).isEqualTo(500L);

        // Ledger E: 신규 적립, 잔액 1000
        ledgerE = ledgerRepository.findByPointKey(pointKeyE).orElseThrow();
        System.out.println("Ledger E (신규): 잔액 " + ledgerE.getBalance() + ", 상태 " + ledgerE.getStatus());
        assertThat(ledgerE.getStatus()).isEqualTo(LedgerStatus.ACTIVE);
        assertThat(ledgerE.getBalance()).isEqualTo(1000L);

        // 전체 트랜잭션 확인
        List<Transaction> allTransactions = transactionRepository.findAll();
        System.out.println("\n총 트랜잭션 수: " + allTransactions.size());
        allTransactions.forEach(t ->
            System.out.println("- " + t.getType() + ": " + t.getAmount() + "원, " + t.getDescription())
        );

        // 총 잔액 = Ledger B (500) + Ledger E (1000) = 1500
        assertThat(wallet.getTotalBalance()).isEqualTo(1500L);

        System.out.println("\n시나리오 테스트 성공!");
    }
}
