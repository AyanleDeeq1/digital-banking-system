package io.github.ayanledeeq1.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;
import io.github.ayanledeeq1.digitalbanking.enums.*;
import io.github.ayanledeeq1.digitalbanking.model.*;
import io.github.ayanledeeq1.digitalbanking.repository.*;
import io.github.ayanledeeq1.digitalbanking.service.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class TransferAtomicityTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @Autowired AccountRespository accounts;
    @Autowired TransactionRepository transactions;
    @MockitoSpyBean LedgerEntryRepository ledger;
    @Autowired AccountBalanceService balances;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManager entityManager;
    private Account[] fixture;
    private Long fixtureCustomer;

    @AfterEach void cleanUp() {
        reset(ledger);
        if (fixture == null) return;
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var accountIds = List.of(fixture[0].getId(), fixture[1].getId());
            var entries = ledger.findAll().stream().filter(entry -> accountIds.contains(entry.getAccount().getId())).toList();
            var transactionIds = entries.stream().map(entry -> entry.getTransaction().getId()).distinct().toList();
            ledger.deleteAll(entries); ledger.flush();
            transactions.deleteAllById(transactionIds); transactions.flush();
            accounts.deleteAllById(accountIds); accounts.flush();
            customers.deleteById(fixtureCustomer); customers.flush();
        });
    }

    @Test void persistenceFailureAfterDebitRollsBackTransactionAndLedger() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        Account[] pair = new TransactionTemplate(manager).execute(status -> {
            Customer customer = customers.save(new Customer("Atomic", "Test", email, new PasswordCredential("hash")));
            fixtureCustomer = customer.getId();
            Account source = new Account("Source", "3424-5,9000000001", AccountType.CHECKING, AccountStatus.ACTIVE);
            Account destination = new Account("Destination", "3424-5,9000000002", AccountType.SAVINGS, AccountStatus.ACTIVE);
            customer.addAccount(source); customer.addAccount(destination);
            accounts.saveAllAndFlush(List.of(source, destination));
            Transaction seed = transactions.save(new Transaction(TransactionType.DEPOSIT, new BigDecimal("100"),
                    Instant.now(), TransactionStatus.COMPLETED));
            ledger.saveAndFlush(new LedgerEntry(new BigDecimal("100"), source, seed));
            return new Account[] {source, destination};
        });
        fixture = pair;
        long beforeTransactions = transactions.count(); long beforeEntries = ledger.count();
        doAnswer(invocation -> {
            List<LedgerEntry> entries = invocation.getArgument(0);
            entityManager.persist(entries.getFirst());
            entityManager.flush();
            throw new DataIntegrityViolationException("Simulated failure after real debit insert");
        }).when(ledger).saveAllAndFlush(any());
        mvc.perform(post("/api/customers/accounts/{id}/transfers", pair[0].getId())
                .with(user(email)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"destinationAccountNumber":"9000000002","amount":40}
                        """))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.massage").value("Unable to complete the transfer. No money was moved"));
        assertEquals(beforeTransactions, transactions.count()); assertEquals(beforeEntries, ledger.count());
        assertEquals(0, balances.getBalance(pair[0].getId()).compareTo(new BigDecimal("100")));
        assertEquals(0, balances.getBalance(pair[1].getId()).compareTo(BigDecimal.ZERO));
    }
}
