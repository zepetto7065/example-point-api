package com.musinsa.point.repository;

import com.musinsa.point.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByPointKey(String pointKey);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}
