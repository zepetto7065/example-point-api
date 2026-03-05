package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    @Column(nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false)
    private Long totalBalance;

    @Version
    private Long version;

    @Builder
    public Wallet(Long memberId, Long totalBalance) {
        this.memberId = memberId;
        this.totalBalance = totalBalance != null ? totalBalance : 0L;
    }

    public void addBalance(long amount) {
        this.totalBalance += amount;
    }

    public void subtractBalance(long amount) {
        if (this.totalBalance < amount) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.totalBalance -= amount;
    }
}
