package io.github.ayanledeeq1.digitalbanking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ayanledeeq1.digitalbanking.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
