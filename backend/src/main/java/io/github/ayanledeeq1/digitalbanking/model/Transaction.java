package io.github.ayanledeeq1.digitalbanking.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import io.github.ayanledeeq1.digitalbanking.enums.TransactionStatus;
import io.github.ayanledeeq1.digitalbanking.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bank_transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    protected Transaction() {}

    public Transaction(TransactionType type, BigDecimal amount, Instant createdAt, TransactionStatus status) {
        this.type = Objects.requireNonNull(type, "Transaction type is required");
        this.amount = MonetaryAmount.normalize(amount);
        if (this.amount.signum() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        this.status = Objects.requireNonNull(status, "Transaction status is required");
    }

    public Long getId() { return id; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
    public TransactionStatus getStatus() { return status; }
}
