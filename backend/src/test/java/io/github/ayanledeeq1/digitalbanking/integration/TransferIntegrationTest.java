package io.github.ayanledeeq1.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import io.github.ayanledeeq1.digitalbanking.dto.transactionDto.TransferRequestDto;
import io.github.ayanledeeq1.digitalbanking.enums.*;
import io.github.ayanledeeq1.digitalbanking.exception.TransferException;
import io.github.ayanledeeq1.digitalbanking.model.*;
import io.github.ayanledeeq1.digitalbanking.repository.*;
import io.github.ayanledeeq1.digitalbanking.service.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @Autowired AccountRespository accounts;
    @Autowired TransactionRepository transactions;
    @Autowired LedgerEntryRepository ledger;
    @Autowired TransferService transfers;
    @Autowired AccountBalanceService balances;
    @Autowired PlatformTransactionManager manager;
    private Customer owner;
    private Account source;
    private Account own;
    private Account other;
    private final List<Long> fixtureAccounts = new ArrayList<>();
    private final List<Long> fixtureCustomers = new ArrayList<>();

    @AfterEach void cleanUp() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var entries = ledger.findAll().stream()
                    .filter(entry -> fixtureAccounts.contains(entry.getAccount().getId())).toList();
            var transactionIds = entries.stream().map(entry -> entry.getTransaction().getId()).distinct().toList();
            ledger.deleteAll(entries); ledger.flush();
            transactions.deleteAllById(transactionIds); transactions.flush();
            accounts.deleteAllById(fixtureAccounts); accounts.flush();
            customers.deleteAllById(fixtureCustomers); customers.flush();
        });
    }

    @BeforeEach void setUp() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            owner = customer();
            source = account(owner, AccountStatus.ACTIVE);
            own = account(owner, AccountStatus.ACTIVE);
            other = account(customer(), AccountStatus.ACTIVE);
            Transaction seed = transactions.save(new Transaction(TransactionType.DEPOSIT,
                    new BigDecimal("100.00"), Instant.now(), TransactionStatus.COMPLETED));
            ledger.saveAndFlush(new LedgerEntry(new BigDecimal("100.00"), source, seed));
        });
    }

    private Customer customer() {
        Customer saved = customers.save(new Customer("Transfer", "Test", UUID.randomUUID() + "@example.com",
                new PasswordCredential("hash")));
        fixtureCustomers.add(saved.getId());
        return saved;
    }

    private Account account(Customer customer, AccountStatus status) {
        String digits = String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L));
        Account account = new Account("Transfer test", "3424-5," + digits, AccountType.SAVINGS, status);
        customer.addAccount(account);
        Account saved = accounts.saveAndFlush(account);
        fixtureAccounts.add(saved.getId());
        return saved;
    }

    private String number(Account account) { return account.getAccountNumber().split(",")[1]; }

    private TransferRequestDto request(Account destination, String amount) {
        return new TransferRequestDto(number(destination), new BigDecimal(amount));
    }

    private void balance(Account account, String expected) {
        assertEquals(0, balances.getBalance(account.getId()).compareTo(new BigDecimal(expected)));
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void transferCreatesOneTransactionAndTwoSignedEntries(boolean sameOwner) throws Exception {
        Account destination = sameOwner ? own : other;
        long beforeTransactions = transactions.count();
        long beforeLedger = ledger.count();
        mvc.perform(post("/api/customers/accounts/{id}/transfers", source.getId())
                .with(user(owner.getEmail())).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationAccountNumber\":\"" + number(destination) + "\",\"amount\":40.000}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(40.0))
                .andExpect(jsonPath("$.createdAt").isString()).andExpect(jsonPath("$.ledgerEntries").doesNotExist());
        assertEquals(beforeTransactions + 1, transactions.count());
        assertEquals(beforeLedger + 2, ledger.count());
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var entries = ledger.findAll().stream().filter(e -> e.getAccount().getId().equals(source.getId())
                    && e.getAmount().signum() < 0).toList();
            assertEquals(1, entries.size());
            var debit = entries.getFirst();
            var credit = ledger.findAll().stream().filter(e -> e.getTransaction().getId()
                    .equals(debit.getTransaction().getId()) && e.getAmount().signum() > 0).findFirst().orElseThrow();
            assertEquals(new BigDecimal("-40.00"), debit.getAmount());
            assertEquals(new BigDecimal("40.00"), credit.getAmount());
            assertEquals(destination.getId(), credit.getAccount().getId());
        });
        balance(source, "60"); balance(destination, "40");
        mvc.perform(get("/api/customers/accounts").with(user(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + source.getId() + ")].balance",
                        org.hamcrest.Matchers.contains(60.0)));
    }

    @Test void exactBalanceIsAllowed() {
        transfers.transfer(owner.getEmail(), source.getId(), request(own, "100"));
        balance(source, "0"); balance(own, "100");
    }

    private void rejected(Long sourceId, String destination, String amount, int httpStatus, String message) throws Exception {
        long transactionCount = transactions.count(); long entryCount = ledger.count();
        mvc.perform(post("/api/customers/accounts/{id}/transfers", sourceId)
                .with(user(owner.getEmail())).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationAccountNumber\":\"" + destination + "\",\"amount\":" + amount + "}"))
                .andExpect(status().is(httpStatus)).andExpect(jsonPath("$.status").value(httpStatus))
                .andExpect(jsonPath("$.massage").value(message));
        assertEquals(transactionCount, transactions.count()); assertEquals(entryCount, ledger.count());
        balance(source, "100"); balance(own, "0"); balance(other, "0");
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1"})
    void nonPositiveAmountsRejected(String amount) throws Exception {
        rejected(source.getId(), number(own), amount, 400, "Amount must be greater than zero");
    }
    @ParameterizedTest @ValueSource(strings = {"1.001", "100000000000000000"})
    void invalidPrecisionRejected(String amount) throws Exception {
        rejected(source.getId(), number(own), amount, 400,
                "Amount must fit 17 integer digits and have at most two meaningful decimal places");
    }
    @Test void insufficientFundsRejected() throws Exception {
        rejected(source.getId(), number(own), "100.01", 409, "Insufficient funds");
    }
    @Test void foreignSourceRejected() throws Exception {
        rejected(other.getId(), number(own), "1", 403, "Source account does not belong to you");
    }
    @Test void missingSourceRejected() throws Exception {
        rejected(Long.MAX_VALUE, number(own), "1", 404, "Source account not found");
    }
    @Test void missingDestinationRejected() throws Exception {
        String missing = "0000000000";
        assertTrue(accounts.findAccountByNumber("3424-5," + missing).isEmpty());
        rejected(source.getId(), missing, "1", 404, "Destination account not found");
    }
    @Test void sameAccountRejected() throws Exception {
        rejected(source.getId(), number(source), "1", 400, "Source and destination must be different accounts");
    }
    @ParameterizedTest @ValueSource(strings = {"FROZEN", "CLOSED"})
    void inactiveAccountsRejected(String accountStatus) throws Exception {
        Account inactive = new TransactionTemplate(manager).execute(status -> account(owner, AccountStatus.valueOf(accountStatus)));
        rejected(inactive.getId(), number(own), "1", 409, "Source account must be ACTIVE");
        rejected(source.getId(), number(inactive), "1", 409, "Destination account must be ACTIVE");
    }
    @ParameterizedTest @ValueSource(strings = {"123", "3424-5,1234567890", "abcdefghij"})
    void invalidRecipientShapeRejected(String recipient) throws Exception {
        rejected(source.getId(), recipient, "1", 400, "Please check the highlighted fields.");
    }
    @Test void authenticationAndCsrfRequired() throws Exception {
        String body = "{\"destinationAccountNumber\":\"" + number(own) + "\",\"amount\":1}";
        long before = ledger.count();
        mvc.perform(post("/api/customers/accounts/{id}/transfers", source.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/customers/accounts/{id}/transfers", source.getId()).with(user(owner.getEmail()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        assertEquals(before, ledger.count());
    }
    @Test void concurrentTransfersCannotOverspend() throws Exception {
        long beforeTransactions = transactions.count(); long beforeEntries = ledger.count();
        CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt = () -> {
                ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS));
                try {
                    transfers.transfer(owner.getEmail(), source.getId(), request(other, "80"));
                    return true;
                } catch (TransferException exception) {
                    assertEquals(TransferException.Reason.INSUFFICIENT_FUNDS, exception.getReason());
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(attempt); Future<Boolean> second = executor.submit(attempt);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            assertNotEquals(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        }
        balance(source, "20"); balance(other, "80");
        assertEquals(beforeTransactions + 1, transactions.count()); assertEquals(beforeEntries + 2, ledger.count());
    }
}
