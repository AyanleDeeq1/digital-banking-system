package io.github.ayanledeeq1.digitalbanking.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountBalanceApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRespository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired LedgerEntryRepository ledgerRepository;
    @Autowired EntityManager entityManager;

    private Customer customer(String email) {
        return customerRepository.save(new Customer("Balance", "Test", email, new PasswordCredential("test-hash")));
    }

    private Account account(Customer customer, String name, String number, AccountType type) {
        Account account = new Account(name, number, type, AccountStatus.ACTIVE);
        customer.addAccount(account);
        return accountRepository.save(account);
    }

    // Seed persistence directly; no financial-operation service or endpoint is introduced.
    private void entry(Account account, String amount) {
        BigDecimal signedAmount = new BigDecimal(amount);
        TransactionType type = signedAmount.signum() > 0 ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL;
        Transaction transaction = transactionRepository.save(new Transaction(
                type, signedAmount.abs(), Instant.now(), TransactionStatus.COMPLETED));
        ledgerRepository.save(new LedgerEntry(signedAmount, account, transaction));
    }

    @Test
    void listsOnlyAuthenticatedCustomersAccountsWithDerivedAndZeroBalances() throws Exception {
        Customer owner = customer("balance-owner@example.com");
        Account checking = account(owner, "Everyday", "3424-5,0000000001", AccountType.CHECKING);
        Account savings = account(owner, "Savings", "3424-5,0000000002", AccountType.SAVINGS);
        Account empty = account(owner, "New account", "3424-5,0000000003", AccountType.CHECKING);
        Account foreign = account(customer("another@example.com"), "Private", "3424-5,0000000004", AccountType.CHECKING);
        entry(checking, "100.25");
        entry(checking, "-30.10");
        entry(savings, "12.34");
        entry(foreign, "999.99");
        entityManager.flush();
        entityManager.clear();

        String checkingPath = "$[?(@.id == " + checking.getId() + ")]";
        mockMvc.perform(get("/api/customers/accounts").with(user(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath(checkingPath + ".name", contains("Everyday")))
                .andExpect(jsonPath(checkingPath + ".accountNumber", contains(checking.getAccountNumber())))
                .andExpect(jsonPath(checkingPath + ".type", contains("CHECKING")))
                .andExpect(jsonPath(checkingPath + ".status", contains("ACTIVE")))
                .andExpect(jsonPath(checkingPath + ".balance", contains(70.15)))
                .andExpect(jsonPath("$[?(@.id == " + savings.getId() + ")].balance", contains(12.34)))
                .andExpect(jsonPath("$[?(@.id == " + empty.getId() + ")].balance", contains(0.0)))
                .andExpect(jsonPath("$[?(@.id == " + foreign.getId() + ")]", hasSize(0)));
    }

    @Test
    void createdAccountResponseAndSubsequentListingIncludeZeroBalance() throws Exception {
        Customer owner = customer("new-account@example.com");
        mockMvc.perform(post("/api/customers/createAccount")
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Holiday savings","accountType":"SAVINGS"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Holiday savings"))
                .andExpect(jsonPath("$.accountNumber").isString())
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").isNumber())
                .andExpect(jsonPath("$.balance").value(0.0));

        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/api/customers/accounts").with(user(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].balance").value(0.0));
    }

    @Test
    void accountBalancesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/customers/accounts"))
                .andExpect(status().is4xxClientError());
    }
}
