package io.github.ayanledeeq1.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import io.github.ayanledeeq1.digitalbanking.enums.*;
import io.github.ayanledeeq1.digitalbanking.exception.AtmException;
import io.github.ayanledeeq1.digitalbanking.model.*;
import io.github.ayanledeeq1.digitalbanking.repository.*;
import io.github.ayanledeeq1.digitalbanking.service.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AtmIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @Autowired AccountRespository accounts;
    @Autowired CardRepository cards;
    @Autowired TransactionRepository transactions;
    @Autowired LedgerEntryRepository ledger;
    @Autowired AccountBalanceService balances;
    @Autowired AtmService atm;
    @Autowired CardPinCipher cipher;
    @Autowired PlatformTransactionManager manager;
    private Customer owner;
    private Card card;
    private Account account;
    private Account foreign;
    private final List<Long> accountIds = new ArrayList<>();
    private final List<Long> customerIds = new ArrayList<>();

    @BeforeEach void setUp() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            owner = customer(); account = account(owner, AccountStatus.ACTIVE);
            foreign = account(customer(), AccountStatus.ACTIVE);
            card = cards.saveAndFlush(new Card(String.format("%016d", account.getId()), "007",
                    LocalDate.now().plusYears(3), cipher.encrypt("0123"), account));
        });
    }
    private Customer customer() {
        Customer saved = customers.save(new Customer("ATM", "Test", UUID.randomUUID() + "@example.com", new PasswordCredential("hash")));
        customerIds.add(saved.getId()); return saved;
    }
    private Account account(Customer customer, AccountStatus status) {
        Account saved = new Account("ATM account", "3424-5," + String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L)),
                AccountType.SAVINGS, status);
        customer.addAccount(saved); accounts.saveAndFlush(saved); accountIds.add(saved.getId()); return saved;
    }
    @AfterEach void cleanUp() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var entries = ledger.findAll().stream().filter(e -> accountIds.contains(e.getAccount().getId())).toList();
            var ids = entries.stream().map(e -> e.getTransaction().getId()).distinct().toList();
            ledger.deleteAll(entries); ledger.flush(); transactions.deleteAllById(ids); transactions.flush();
            if (card != null) { cards.deleteById(card.getId()); cards.flush(); }
            accounts.deleteAllById(accountIds); accounts.flush(); customers.deleteAllById(customerIds); customers.flush();
        });
    }
    private MockHttpSession verified() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/customers/atm/pin").session(session).with(user(owner.getEmail())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"0123\"}"))
                .andExpect(status().isNoContent());
        return session;
    }
    private void balance(String expected) { assertEquals(0, balances.getBalance(account.getId()).compareTo(new BigDecimal(expected))); }

    @Test void correctPinAcceptedAndCiphertextNeverExposed() throws Exception {
        verified();
        String stored = cards.findById(card.getId()).orElseThrow().getEncryptedPin();
        assertNotEquals("0123", stored); assertTrue(cipher.matches("0123", stored));
        mvc.perform(get("/api/customers/card").with(user(owner.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pin").doesNotExist())
                .andExpect(jsonPath("$.encryptedPin").doesNotExist());
    }
    @Test void registrationCardAllowsAccessToAdditionalOwnedAccountWithoutCard() throws Exception {
        Account extra = new TransactionTemplate(manager).execute(status -> account(owner, AccountStatus.ACTIVE));
        assertTrue(cards.findByAccountId(extra.getId()).isEmpty());
        mvc.perform(post("/api/customers/atm/pin").with(user(owner.getEmail())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"9876\"}"))
                .andExpect(status().isUnauthorized());
        MockHttpSession session = verified();
        attempt(session, extra.getId(), "deposits", "10", 201);
        assertEquals(0, balances.getBalance(extra.getId()).compareTo(new BigDecimal("10")));
        balance("0");
    }
    @Test void wrongPinRevokesVerificationAndEjectRequiresVerificationAgain() throws Exception {
        MockHttpSession session = verified();
        mvc.perform(post("/api/customers/atm/pin").session(session).with(user(owner.getEmail())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"9999\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.massage").value("Incorrect PIN. Please try again"));
        attempt(session, account.getId(), "deposits", "1", 403);
        session = verified();
        mvc.perform(post("/api/customers/atm/eject").session(session).with(user(owner.getEmail())).with(csrf()))
                .andExpect(status().isNoContent());
        attempt(session, account.getId(), "withdrawals", "1", 403);
        assertEquals(0, ledger.count()); assertEquals(0, transactions.count());
    }
    private void attempt(MockHttpSession session, Long id, String operation, String amount, int expected) throws Exception {
        mvc.perform(post("/api/customers/atm/accounts/{id}/{operation}", id, operation)
                .session(session).with(user(owner.getEmail())).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amount + "}"))
                .andExpect(status().is(expected));
    }
    @Test void depositAndWithdrawPersistSignedEntriesAndUpdateBalances() throws Exception {
        MockHttpSession session = verified();
        for (String operation : List.of("deposits", "withdrawals")) {
            String type = operation.equals("deposits") ? "DEPOSIT" : "WITHDRAWAL";
            mvc.perform(post("/api/customers/atm/accounts/{id}/{operation}", account.getId(), operation)
                    .session(session).with(user(owner.getEmail())).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":40.000}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value(type))
                    .andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.amount").value(40.0));
            balance(operation.equals("deposits") ? "40" : "0");
        }
        assertEquals(2, transactions.count()); assertEquals(2, ledger.count());
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            for (LedgerEntry entry : ledger.findAll()) {
                BigDecimal expected = entry.getTransaction().getType() == TransactionType.DEPOSIT
                        ? new BigDecimal("40.00") : new BigDecimal("-40.00");
                assertEquals(expected, entry.getAmount()); assertEquals(account.getId(), entry.getAccount().getId());
            }
        });
    }
    @ParameterizedTest @ValueSource(strings = {"deposits", "withdrawals"})
    void validationFailuresLeaveNoFinancialRecords(String operation) throws Exception {
        MockHttpSession session = verified();
        for (String amount : List.of("0", "-1", "1.001", "100000000000000000", "null")) {
            attempt(session, account.getId(), operation, amount, 400);
        }
        attempt(session, foreign.getId(), operation, "1", 403);
        attempt(session, Long.MAX_VALUE, operation, "1", 404);
        for (AccountStatus accountStatus : List.of(AccountStatus.FROZEN, AccountStatus.CLOSED)) {
            Account inactive = new TransactionTemplate(manager).execute(status -> account(owner, accountStatus));
            attempt(session, inactive.getId(), operation, "1", 409);
        }
        assertEquals(0, ledger.count()); assertEquals(0, transactions.count()); balance("0");
    }
    @Test void withdrawalCannotOverdraw() throws Exception {
        MockHttpSession session = verified();
        attempt(session, account.getId(), "deposits", "50", 201);
        mvc.perform(post("/api/customers/atm/accounts/{id}/withdrawals", account.getId()).session(session)
                .with(user(owner.getEmail())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":50.01}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.massage").value("Insufficient funds"));
        assertEquals(1, ledger.count()); assertEquals(1, transactions.count()); balance("50");
    }
    @Test void authenticationCsrfAndVerifiedSessionRequired() throws Exception {
        attempt(new MockHttpSession(), account.getId(), "deposits", "1", 403);
        mvc.perform(post("/api/customers/atm/pin").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pin\":\"0123\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/customers/atm/pin").with(user(owner.getEmail())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pin\":\"0123\"}")).andExpect(status().isForbidden());
        MockHttpSession session = verified();
        mvc.perform(post("/api/customers/atm/accounts/{id}/deposits", account.getId()).session(session)
                .with(user(owner.getEmail())).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/customers/atm/accounts/{id}/deposits", foreign.getId()).session(session)
                .with(user(foreign.getCustomer().getEmail())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}"))
                .andExpect(status().isNotFound()); // Other customer has no card; cannot borrow this verification.
        assertEquals(0, ledger.count()); assertEquals(0, transactions.count());
    }
    @Test void concurrentWithdrawalsCannotOverdraw() throws Exception {
        atm.deposit(owner.getEmail(), card.getId(), account.getId(), new BigDecimal("100"));
        CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt = () -> {
                ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS));
                try { atm.withdraw(owner.getEmail(), card.getId(), account.getId(), new BigDecimal("80")); return true; }
                catch (AtmException failure) { assertEquals(AtmException.Reason.INSUFFICIENT_FUNDS, failure.getReason()); return false; }
            };
            var first = executor.submit(attempt); var second = executor.submit(attempt);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            assertNotEquals(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        }
        balance("20"); assertEquals(2, transactions.count()); assertEquals(2, ledger.count());
    }
    @Test void verifiedCardCannotBeBorrowedByAnotherCustomerWithOwnCard() throws Exception {
        MockHttpSession session = verified();
        Card foreignCard = new TransactionTemplate(manager).execute(status -> cards.saveAndFlush(
                new Card(String.format("%016d", foreign.getId()), "007", LocalDate.now().plusYears(3), cipher.encrypt("9876"), foreign)));
        try {
            mvc.perform(post("/api/customers/atm/accounts/{id}/deposits", foreign.getId()).session(session)
                    .with(user(foreign.getCustomer().getEmail())).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":1}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.massage").value("Insert your card and verify your PIN first"));
            assertEquals(0, ledger.count());
            assertEquals(0, transactions.count());
        } finally {
            new TransactionTemplate(manager).executeWithoutResult(status -> {
                cards.deleteById(foreignCard.getId());
                cards.flush();
            });
        }
    }
}
