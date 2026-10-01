package io.github.ayanledeeq1.digitalbanking.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import io.github.ayanledeeq1.digitalbanking.enums.AccountType;
import io.github.ayanledeeq1.digitalbanking.enums.TransactionStatus;
import io.github.ayanledeeq1.digitalbanking.enums.TransactionType;

class FinancialAmountTest {
    private final Account account = new Account("Test", "3424-5,0000000001",
            AccountType.CHECKING, AccountStatus.ACTIVE);

    private Transaction transaction(BigDecimal amount) {
        return new Transaction(TransactionType.DEPOSIT, amount, Instant.now(), TransactionStatus.COMPLETED);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1", "1.234", "0.001", "100000000000000000.00"})
    void rejectsInvalidTransactionAmounts(String value) {
        BigDecimal amount = value == null ? null : new BigDecimal(value);
        assertThrows(IllegalArgumentException.class, () -> transaction(amount));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "1.234", "-1.234", "100000000000000000.00", "-100000000000000000.00"})
    void rejectsInvalidLedgerAmounts(String value) {
        BigDecimal amount = value == null ? null : new BigDecimal(value);
        Transaction transaction = transaction(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> new LedgerEntry(amount, account, transaction));
    }

    @Test
    void normalizesTrailingZerosWithoutChangingValue() {
        Transaction transaction = transaction(new BigDecimal("1.230"));
        assertEquals(new BigDecimal("1.23"), transaction.getAmount());
        assertEquals(new BigDecimal("-1.23"),
                new LedgerEntry(new BigDecimal("-1.2300"), account, transaction).getAmount());
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus.class, names = {"PENDING", "FAILED"})
    void rejectsLedgerEntriesForUnsuccessfulTransactions(TransactionStatus status) {
        Transaction transaction = new Transaction(TransactionType.DEPOSIT, BigDecimal.ONE, Instant.now(), status);
        assertThrows(IllegalArgumentException.class, () -> new LedgerEntry(BigDecimal.ONE, account, transaction));
    }

    @Test
    void requiresBothLedgerRelationships() {
        Transaction transaction = transaction(BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> new LedgerEntry(BigDecimal.ONE, null, transaction));
        assertThrows(NullPointerException.class, () -> new LedgerEntry(BigDecimal.ONE, account, null));
    }
}
