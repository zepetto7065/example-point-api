package com.musinsa.point.domain;

import com.musinsa.point.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(nullable = false, unique = true, updatable = false)
    private String pointKey;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Long amount;

    private String orderId;

    private String relatedPointKey;

    private String description;

    @Column(unique = true)
    private String idempotencyKey;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Transaction(Long memberId, TransactionType type, Long amount,
                            String orderId, String relatedPointKey, String description,
                            String idempotencyKey) {
        this.pointKey = UUID.randomUUID().toString();
        this.memberId = memberId;
        this.type = type;
        this.amount = amount;
        this.orderId = orderId;
        this.relatedPointKey = relatedPointKey;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
    }
}
