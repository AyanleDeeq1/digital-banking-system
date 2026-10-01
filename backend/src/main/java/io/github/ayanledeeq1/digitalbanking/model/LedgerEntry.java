package io.github.ayanledeeq1.digitalbanking.model;

import java.math.BigDecimal;
import java.util.Objects;

import io.github.ayanledeeq1.digitalbanking.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    protected LedgerEntry() {}

    public LedgerEntry(BigDecimal amount, Account account, Transaction transaction) {
        this.amount = MonetaryAmount.normalize(amount);
        if (this.amount.signum() == 0) {
            throw new IllegalArgumentException("Ledger amount must be non-zero");
        }
        this.account = Objects.requireNonNull(account, "Account is required");
        this.transaction = Objects.requireNonNull(transaction, "Transaction is required");
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed transactions may have ledger entries");
        }
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public Account getAccount() { return account; }
    public Transaction getTransaction() { return transaction; }
}
