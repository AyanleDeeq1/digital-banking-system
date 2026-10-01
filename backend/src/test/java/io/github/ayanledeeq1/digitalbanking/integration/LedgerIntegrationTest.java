package io.github.ayanledeeq1.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import io.github.ayanledeeq1.digitalbanking.enums.AccountType;
import io.github.ayanledeeq1.digitalbanking.enums.TransactionStatus;
import io.github.ayanledeeq1.digitalbanking.enums.TransactionType;
import io.github.ayanledeeq1.digitalbanking.model.Account;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import io.github.ayanledeeq1.digitalbanking.model.LedgerEntry;
import io.github.ayanledeeq1.digitalbanking.model.PasswordCredential;
import io.github.ayanledeeq1.digitalbanking.model.Transaction;
import io.github.ayanledeeq1.digitalbanking.repository.AccountRespository;
import io.github.ayanledeeq1.digitalbanking.repository.CustomerRepository;
import io.github.ayanledeeq1.digitalbanking.repository.LedgerEntryRepository;
import io.github.ayanledeeq1.digitalbanking.repository.TransactionRepository;
import io.github.ayanledeeq1.digitalbanking.service.AccountBalanceService;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LedgerIntegrationTest {
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRespository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired LedgerEntryRepository ledgerRepository;
    @Autowired AccountBalanceService balanceService;
    @Autowired EntityManager entityManager;

    private Account first;
    private Account second;
    private Account empty;

    @BeforeEach
    void setUp() {
        Customer customer = customerRepository.save(new Customer("Ledger", "Test", "ledger@example.com",
                new PasswordCredential("test-hash")));
        first = account(customer, "3424-5,0000000001");
        second = account(customer, "3424-5,0000000002");
        empty = account(customer, "3424-5,0000000003");
    }

    private Account account(Customer customer, String number) {
        Account account = new Account("Test account", number, AccountType.CHECKING, AccountStatus.ACTIVE);
        customer.addAccount(account);
        return accountRepository.save(account);
    }

    private Transaction transaction(TransactionType type, String amount) {
        return transactionRepository.save(new Transaction(type, new BigDecimal(amount),
                Instant.parse("2026-01-01T12:00:00Z"), TransactionStatus.COMPLETED));
    }

    private void entry(Account account, String amount) {
        BigDecimal signedAmount = new BigDecimal(amount);
        TransactionType type = signedAmount.signum() > 0 ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL;
        ledgerRepository.save(new LedgerEntry(signedAmount, account,
                transaction(type, signedAmount.abs().toPlainString())));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void assertBalance(String expected, Account account) {
        assertEquals(0, new BigDecimal(expected).compareTo(balanceService.getBalance(account.getId())));
    }

    @Test
    void accountWithoutEntriesHasZeroBalance() {
        flushAndClear();
        assertBalance("0.00", empty);
    }

    @Test
    void positiveEntryIncreasesBalance() {
        entry(first, "100.00");
        flushAndClear();
        assertBalance("100.00", first);
    }

    @Test
    void negativeEntryDecreasesBalance() {
        entry(first, "100.00");
        entry(first, "-30.00");
        flushAndClear();
        assertBalance("70.00", first);
    }

    @Test
    void multipleEntriesSumExactlyAndStayIsolatedByAccount() {
        entry(first, "100.10");
        entry(first, "-30.20");
        entry(first, "0.10");
        entry(second, "500.00");
        flushAndClear();
        assertBalance("70.00", first);
        assertBalance("500.00", second);
        assertBalance("0.00", empty);
    }

    @Test
    void groupedBalancesIncludeEmptyAccountsAndExcludeUnrequestedAccounts() {
        entry(first, "20.00");
        entry(first, "-5.00");
        entry(second, "99.00");
        flushAndClear();
        Map<Long, BigDecimal> balances = balanceService.getBalances(List.of(first.getId(), empty.getId()));
        assertEquals(2, balances.size());
        assertEquals(0, new BigDecimal("15.00").compareTo(balances.get(first.getId())));
        assertEquals(0, BigDecimal.ZERO.compareTo(balances.get(empty.getId())));
        assertTrue(balanceService.getBalances(List.of()).isEmpty());
    }

    @Test
    void transactionCanPersistWithoutLedgerEntries() {
        Transaction saved = transaction(TransactionType.DEPOSIT, "1.00");
        flushAndClear();
        assertTrue(transactionRepository.findById(saved.getId()).isPresent());
        assertEquals(0, ledgerRepository.count());
    }

    @Test
    void sharedTransactionAndSignedEntriesRoundTripThroughDatabase() {
        Transaction transaction = transaction(TransactionType.TRANSFER, "1.230");
        LedgerEntry outgoing = ledgerRepository.save(new LedgerEntry(new BigDecimal("-1.230"), first, transaction));
        LedgerEntry incoming = ledgerRepository.save(new LedgerEntry(new BigDecimal("1.230"), second, transaction));
        flushAndClear();

        Transaction loaded = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(TransactionType.TRANSFER, loaded.getType());
        assertEquals(TransactionStatus.COMPLETED, loaded.getStatus());
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), loaded.getCreatedAt());
        assertEquals(new BigDecimal("1.23"), loaded.getAmount());

        LedgerEntry debit = ledgerRepository.findById(outgoing.getId()).orElseThrow();
        LedgerEntry credit = ledgerRepository.findById(incoming.getId()).orElseThrow();
        assertEquals(new BigDecimal("-1.23"), debit.getAmount());
        assertEquals(new BigDecimal("1.23"), credit.getAmount());
        assertEquals(first.getId(), debit.getAccount().getId());
        assertEquals(second.getId(), credit.getAccount().getId());
        assertEquals(loaded.getId(), debit.getTransaction().getId());
        assertEquals(loaded.getId(), credit.getTransaction().getId());
    }

    @Test
    void decimal19Scale2BoundariesPersistExactly() {
        String maximum = "99999999999999999.99";
        Transaction transaction = transaction(TransactionType.TRANSFER, maximum);
        LedgerEntry positive = ledgerRepository.save(new LedgerEntry(new BigDecimal(maximum), first, transaction));
        LedgerEntry negative = ledgerRepository.save(new LedgerEntry(new BigDecimal("-" + maximum), second, transaction));
        flushAndClear();
        assertEquals(new BigDecimal(maximum), transactionRepository.findById(transaction.getId()).orElseThrow().getAmount());
        assertEquals(new BigDecimal(maximum), ledgerRepository.findById(positive.getId()).orElseThrow().getAmount());
        assertEquals(new BigDecimal("-" + maximum), ledgerRepository.findById(negative.getId()).orElseThrow().getAmount());
    }

    @Test
    void aggregateBalanceIsNotLimitedToSingleEntryStoragePrecision() {
        entry(first, "99999999999999999.99");
        entry(first, "0.01");
        flushAndClear();
        assertBalance("100000000000000000.00", first);
    }
}
