package io.github.ayanledeeq1.digitalbanking.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import io.github.ayanledeeq1.digitalbanking.enums.*;
import io.github.ayanledeeq1.digitalbanking.model.*;
import io.github.ayanledeeq1.digitalbanking.repository.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionHistoryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @Autowired AccountRespository accounts;
    @Autowired TransactionRepository transactions;
    @Autowired LedgerEntryRepository ledger;

    private Customer customer(String email) {
        return customers.save(new Customer("History", "Test", email, new PasswordCredential("test-hash")));
    }

    private Account account(Customer owner, String number) {
        var account = new Account("Account " + number, number, AccountType.CHECKING, AccountStatus.ACTIVE);
        owner.addAccount(account);
        return accounts.save(account);
    }

    private Transaction transaction(TransactionType type, String amount, String time) {
        return transactions.save(new Transaction(type, new BigDecimal(amount), Instant.parse(time), TransactionStatus.COMPLETED));
    }

    @Test
    void accountHistoryUsesSignedLedgerAmountsAndNewestFirst() throws Exception {
        var owner = customer("history-owner@example.com");
        var source = account(owner, "history-source");
        var destination = account(owner, "history-destination");
        var deposit = transaction(TransactionType.DEPOSIT, "1000", "2026-10-01T10:00:00Z");
        ledger.save(new LedgerEntry(new BigDecimal("1000"), source, deposit));
        var transfer = transaction(TransactionType.TRANSFER, "500", "2026-10-02T10:00:00Z");
        ledger.save(new LedgerEntry(new BigDecimal("-500"), source, transfer));
        ledger.save(new LedgerEntry(new BigDecimal("500"), destination, transfer));
        var withdrawal = transaction(TransactionType.WITHDRAWAL, "100", "2026-10-03T10:00:00Z");
        ledger.saveAndFlush(new LedgerEntry(new BigDecimal("-100"), source, withdrawal));

        mvc.perform(get("/api/customers/accounts/{id}/transactions", source.getId()).with(user(owner.getEmail())))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[0].amount").value(-100))
                .andExpect(jsonPath("$[1].type").value("TRANSFER"))
                .andExpect(jsonPath("$[1].amount").value(-500))
                .andExpect(jsonPath("$[2].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[2].amount").value(1000))
                .andExpect(jsonPath("$[0].accountId").value(source.getId()))
                .andExpect(jsonPath("$[0].accountName").value(source.getAccountName()))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].balance").doesNotExist())
                .andExpect(jsonPath("$[0].customer").doesNotExist());
        mvc.perform(get("/api/customers/accounts/{id}/transactions", destination.getId()).with(user(owner.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount").value(500))
                .andExpect(jsonPath("$[0].transactionId").value(transfer.getId()));
    }

    @Test
    void rejectsForeignAndMissingAccountsAndReturnsEmptyOwnedHistory() throws Exception {
        var owner = customer("empty-history@example.com");
        var empty = account(owner, "history-empty");
        var foreign = account(customer("foreign-history@example.com"), "history-private");
        mvc.perform(get("/api/customers/accounts/{id}/transactions", empty.getId()).with(user(owner.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/customers/accounts/{id}/transactions", foreign.getId()).with(user(owner.getEmail())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.massage").value("Account does not belong to you"));
        mvc.perform(get("/api/customers/accounts/{id}/transactions", Long.MAX_VALUE).with(user(owner.getEmail())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/api/customers/transactions/recent").with(user(owner.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void recentActivityLimitsToFiveOwnedMovementsAndPreservesBothTransferSides() throws Exception {
        var owner = customer("recent-history@example.com");
        var source = account(owner, "recent-source");
        var destination = account(owner, "recent-destination");
        var foreign = account(customer("private-recent@example.com"), "recent-private");
        for (int i = 0; i < 6; i++) {
            var deposit = transaction(TransactionType.DEPOSIT, "10", "2026-10-01T10:00:00Z");
            ledger.save(new LedgerEntry(new BigDecimal("10"), source, deposit));
        }
        var internal = transaction(TransactionType.TRANSFER, "5", "2026-10-02T10:00:00Z");
        ledger.save(new LedgerEntry(new BigDecimal("-5"), source, internal));
        ledger.save(new LedgerEntry(new BigDecimal("5"), destination, internal));
        var external = transaction(TransactionType.TRANSFER, "2", "2026-10-03T10:00:00Z");
        ledger.save(new LedgerEntry(new BigDecimal("-2"), source, external));
        ledger.save(new LedgerEntry(new BigDecimal("2"), foreign, external));
        var privateDeposit = transaction(TransactionType.DEPOSIT, "999", "2026-10-04T10:00:00Z");
        ledger.saveAndFlush(new LedgerEntry(new BigDecimal("999"), foreign, privateDeposit));
        mvc.perform(get("/api/customers/transactions/recent").with(user(owner.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].transactionId").value(external.getId()))
                .andExpect(jsonPath("$[0].amount").value(-2))
                .andExpect(jsonPath("$[1].transactionId").value(internal.getId()))
                .andExpect(jsonPath("$[1].accountId").value(destination.getId()))
                .andExpect(jsonPath("$[1].amount").value(5))
                .andExpect(jsonPath("$[2].transactionId").value(internal.getId()))
                .andExpect(jsonPath("$[2].accountId").value(source.getId()))
                .andExpect(jsonPath("$[2].amount").value(-5))
                .andExpect(jsonPath("$[?(@.accountId == " + foreign.getId() + ")]", hasSize(0)));
    }

    @Test
    void bothEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/customers/accounts/1/transactions")).andExpect(status().isForbidden());
        mvc.perform(get("/api/customers/transactions/recent")).andExpect(status().isForbidden());
    }
}
