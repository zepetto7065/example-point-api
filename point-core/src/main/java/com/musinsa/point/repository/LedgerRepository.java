package com.musinsa.point.repository;

import com.musinsa.point.domain.Ledger;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    Optional<Ledger> findByPointKey(String pointKey);

    Page<Ledger> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 사용 시 차감 순서: ADMIN_MANUAL 우선, 만료일 짧은 순
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Ledger l WHERE l.memberId = :memberId " +
            "AND l.status = 'ACTIVE' AND l.balance > 0 AND l.expireAt > :now " +
            "ORDER BY CASE l.earnType WHEN 'ADMIN_MANUAL' THEN 0 ELSE 1 END, l.expireAt ASC")
    List<Ledger> findDeductibleLedgers(@Param("memberId") Long memberId,
                                            @Param("now") LocalDateTime now);

    @Query("SELECT l FROM Ledger l WHERE l.status = 'ACTIVE' AND l.expireAt <= :now AND l.balance > 0")
    List<Ledger> findExpiredLedgers(@Param("now") LocalDateTime now);

}
