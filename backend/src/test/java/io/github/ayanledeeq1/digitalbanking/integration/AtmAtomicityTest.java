package io.github.ayanledeeq1.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto;
import io.github.ayanledeeq1.digitalbanking.model.Card;
import io.github.ayanledeeq1.digitalbanking.repository.*;
import io.github.ayanledeeq1.digitalbanking.service.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AtmAtomicityTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerService customerService;
    @Autowired CustomerRepository customers;
    @Autowired AccountRespository accounts;
    @Autowired CardRepository cards;
    @Autowired TransactionRepository transactions;
    @MockitoSpyBean LedgerEntryRepository ledger;
    @Autowired AtmService atm;
    @Autowired AccountBalanceService balances;
    @Autowired CardPinCipher cipher;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager manager;
    private Long customerId;
    private Long accountId;
    private Long cardId;

    @AfterEach void cleanUp() {
        reset(ledger);
        if (customerId == null) return;
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var entries = ledger.findAll().stream().filter(e -> e.getAccount().getId().equals(accountId)).toList();
            var ids = entries.stream().map(e -> e.getTransaction().getId()).distinct().toList();
            ledger.deleteAll(entries); ledger.flush(); transactions.deleteAllById(ids); transactions.flush();
            cards.deleteById(cardId); cards.flush(); accounts.deleteById(accountId); accounts.flush();
            customers.deleteById(customerId); customers.flush();
        });
    }

    @ParameterizedTest @ValueSource(strings = {"deposits", "withdrawals"})
    void failureAfterRealLedgerInsertRollsBackTransactionAndEntry(String operation) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        customerId = customerService.saveCustomer(new RegisterCustomerDto("ATM", "Atomic", email, "ValidPass1!")).getId();
        Card card = new TransactionTemplate(manager).execute(status -> cards.findFirstByAccountCustomerEmailOrderByAccountIdAsc(email).orElseThrow());
        cardId = card.getId(); accountId = card.getAccount().getId();
        atm.deposit(email, cardId, accountId, new BigDecimal("100"));
        long beforeTransactions = transactions.count(); long beforeLedger = ledger.count();
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/customers/atm/pin").session(session).with(user(email)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"" + cipher.decrypt(card.getEncryptedPin()) + "\"}"))
                .andExpect(status().isNoContent());
        doAnswer(invocation -> {
            entityManager.persist(invocation.getArgument(0)); entityManager.flush();
            throw new DataIntegrityViolationException("Simulated failure after actual ledger insert");
        }).when(ledger).saveAndFlush(any());
        mvc.perform(post("/api/customers/atm/accounts/{id}/{operation}", accountId, operation)
                .session(session).with(user(email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":40}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.massage").value("Unable to complete the operation. No money was moved"));
        assertEquals(beforeTransactions, transactions.count()); assertEquals(beforeLedger, ledger.count());
        assertEquals(0, balances.getBalance(accountId).compareTo(new BigDecimal("100")));
    }
}
