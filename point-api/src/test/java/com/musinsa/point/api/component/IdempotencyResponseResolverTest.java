package com.musinsa.point.api.component;

import com.musinsa.point.api.controller.payload.response.EarnResponse;
import com.musinsa.point.api.controller.payload.response.TransactionResponse;
import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.TransactionType;
import com.musinsa.point.repository.LedgerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyResponseResolver 단위 테스트")
class IdempotencyResponseResolverTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @InjectMocks
    private EarnResponseResolver earnResponseResolver;

    private final TransactionResponseResolver transactionResponseResolver = new TransactionResponseResolver();

    @Test
    @DisplayName("EarnResponseResolver - Transaction의 relatedPointKey로 Ledger 조회 후 EarnResponse 변환")
    void earnResponseResolver_success() {
        // given
        String relatedPointKey = "pk-123";
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .memberId(100L)
                .type(TransactionType.EARN)
                .amount(500L)
                .relatedPointKey(relatedPointKey)
                .build();

        Ledger ledger = Ledger.builder()
                .memberId(100L)
                .earnAmount(500L)
                .earnType(EarnType.NORMAL)
                .description("적립 테스트")
                .startAt(now)
                .expireAt(now.plusYears(1))
                .build();

        given(ledgerRepository.findByPointKey(relatedPointKey))
                .willReturn(Optional.of(ledger));

        // when
        EarnResponse result = earnResponseResolver.resolve(transaction);

        // then
        assertThat(result).isNotNull();
        assertThat(result.memberId()).isEqualTo(100L);
        assertThat(result.earnAmount()).isEqualTo(500L);
        assertThat(result.earnType()).isEqualTo(EarnType.NORMAL);
        assertThat(result.description()).isEqualTo("적립 테스트");
        assertThat(result.expireAt()).isEqualTo(now.plusYears(1));
    }

    @Test
    @DisplayName("EarnResponseResolver - Ledger 없으면 NoSuchElementException 발생")
    void earnResponseResolver_ledgerNotFound() {
        // given
        String relatedPointKey = "pk-not-exist";
        Transaction transaction = Transaction.builder()
                .memberId(100L)
                .type(TransactionType.EARN)
                .amount(500L)
                .relatedPointKey(relatedPointKey)
                .build();

        given(ledgerRepository.findByPointKey(relatedPointKey))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> earnResponseResolver.resolve(transaction))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("TransactionResponseResolver - Transaction을 TransactionResponse로 변환")
    void transactionResponseResolver_success() {
        // given
        Transaction transaction = Transaction.builder()
                .memberId(100L)
                .type(TransactionType.USE)
                .amount(300L)
                .orderId("order-123")
                .description("사용 테스트")
                .build();

        // when
        TransactionResponse result = transactionResponseResolver.resolve(transaction);

        // then
        assertThat(result).isNotNull();
        assertThat(result.memberId()).isEqualTo(100L);
        assertThat(result.type()).isEqualTo(TransactionType.USE);
        assertThat(result.amount()).isEqualTo(300L);
        assertThat(result.orderId()).isEqualTo("order-123");
        assertThat(result.description()).isEqualTo("사용 테스트");
        assertThat(result.pointKey()).isNotNull();
    }
}
