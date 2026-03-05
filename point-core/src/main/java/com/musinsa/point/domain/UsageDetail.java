package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usage_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageDetail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usageDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "use_transaction_id", nullable = false)
    private Transaction useTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long cancelAmount;

    @Builder
    public UsageDetail(Transaction useTransaction, Ledger ledger, Long amount) {
        this.useTransaction = useTransaction;
        this.ledger = ledger;
        this.amount = amount;
        this.cancelAmount = 0L;
    }

    public long getCancellableAmount() {
        return this.amount - this.cancelAmount;
    }

    public void addCancelAmount(long amount) {
        if (this.cancelAmount + amount > this.amount) {
            throw new IllegalStateException("취소 금액이 사용 금액을 초과합니다.");
        }
        this.cancelAmount += amount;
    }
}
