package com.musinsa.point.domain;

import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.LedgerStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ledger extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ledgerId;

    @Column(nullable = false, unique = true, updatable = false)
    private String pointKey;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long earnAmount;

    @Column(nullable = false)
    private Long balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EarnType earnType;

    private String description;

    private String userId;

    private Long sourceTransactionId;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerStatus status;

    @Builder
    public Ledger(Long memberId, Long earnAmount, EarnType earnType,
                       String description, String userId, Long sourceTransactionId,
                       LocalDateTime startAt, LocalDateTime expireAt) {
        this.pointKey = UUID.randomUUID().toString();
        this.memberId = memberId;
        this.earnAmount = earnAmount;
        this.balance = earnAmount;
        this.earnType = earnType;
        this.description = description;
        this.userId = userId;
        this.sourceTransactionId = sourceTransactionId;
        this.startAt = startAt;
        this.expireAt = expireAt;
        this.status = LedgerStatus.ACTIVE;
    }

    public void use(long amount) {
        if (this.balance < amount) {
            throw new IllegalStateException("적립건 잔액이 부족합니다.");
        }
        this.balance -= amount;
    }

    public void restore(long amount) {
        this.balance += amount;
    }

    public void cancel() {
        this.status = LedgerStatus.CANCELED;
        this.balance = 0L;
    }

    public void expire() {
        this.status = LedgerStatus.EXPIRED;
        this.balance = 0L;
    }

    public boolean isActive() {
        return this.status == LedgerStatus.ACTIVE;
    }

    public boolean isExpired() {
        return this.status == LedgerStatus.EXPIRED;
    }

    public long getAvailableBalance() {
        return isActive() ? this.balance : 0L;
    }
}
