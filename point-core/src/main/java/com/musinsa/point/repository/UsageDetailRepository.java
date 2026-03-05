package com.musinsa.point.repository;

import com.musinsa.point.domain.UsageDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsageDetailRepository extends JpaRepository<UsageDetail, Long> {

    @Query("SELECT ud FROM UsageDetail ud JOIN FETCH ud.ledger " +
            "WHERE ud.useTransaction.transactionId = :transactionId " +
            "ORDER BY ud.usageDetailId ASC")
    List<UsageDetail> findByUseTransactionIdWithLedger(@Param("transactionId") Long transactionId);

    @Query("SELECT ud FROM UsageDetail ud JOIN FETCH ud.ledger JOIN FETCH ud.useTransaction t " +
            "WHERE t.orderId = :orderId ORDER BY ud.usageDetailId ASC")
    List<UsageDetail> findByOrderIdWithLedgerAndTransaction(@Param("orderId") String orderId);
}
