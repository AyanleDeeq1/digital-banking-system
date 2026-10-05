package io.github.ayanledeeq1.digitalbanking.integration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import io.github.ayanledeeq1.digitalbanking.repository.CardRepository;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardRegistrationAtomicityTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @MockitoSpyBean CardRepository cards;
    @Autowired io.github.ayanledeeq1.digitalbanking.service.CustomerService customers;

    @Test void failureAfterCardInsertRollsBackEveryRegistrationRecord() throws Exception {
        List<String> tables = List.of("customer", "password_credential", "account", "card");
        List<Long> before = tables.stream().map(t -> jdbc.queryForObject("select count(*) from " + t, Long.class)).toList();
        doAnswer(invocation -> {
            entityManager.persist(invocation.getArgument(0));
            entityManager.flush(); // Flush real inserts before simulating the failure.
            throw new DataIntegrityViolationException("Simulated card persistence failure");
        }).when(cards).saveAndFlush(any());

        mvc.perform(post("/api/customers").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"Rollback","lastName":"Test","email":"card-rollback@example.com","password":"ValidPass1!"}
                        """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.massage").value("Unable to issue a debit card. Registration was not completed"));
        // No enclosing test transaction: these reads inspect the database after request rollback.
        for (int i = 0; i < tables.size(); i++) {
            assertEquals(before.get(i), jdbc.queryForObject("select count(*) from " + tables.get(i), Long.class));
        }
    }

    @Test void additionalAccountCreationDoesNotAttemptCardIssuance() {
        String email = "additional-card-rollback@example.com";
        customers.saveCustomer(new io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto(
                "Rollback", "Test", email, "ValidPass1!"));
        List<String> tables = List.of("customer", "password_credential", "account", "card");
        List<Long> before = tables.stream().map(t -> jdbc.queryForObject("select count(*) from " + t, Long.class)).toList();
        org.mockito.Mockito.clearInvocations(cards);
        try {
            doAnswer(invocation -> {
                entityManager.persist(invocation.getArgument(0));
                entityManager.flush();
                throw new DataIntegrityViolationException("Simulated card persistence failure");
            }).when(cards).saveAndFlush(any());
            var extra = customers.createAnotherAccount(new io.github.ayanledeeq1.digitalbanking.dto.accountDto.AccountCreateDto(
                            "Extra", io.github.ayanledeeq1.digitalbanking.enums.AccountType.SAVINGS), email);
            org.mockito.Mockito.verify(cards, org.mockito.Mockito.never()).saveAndFlush(any());
            assertTrue(cards.findByAccountId(extra.getId()).isEmpty());
            for (int i = 0; i < tables.size(); i++) {
                long expected = before.get(i) + (tables.get(i).equals("account") ? 1 : 0);
                assertEquals(expected, jdbc.queryForObject("select count(*) from " + tables.get(i), Long.class));
            }
        } finally {
            Long credentialId = jdbc.queryForObject("select password_credential_id from customer where email = ?", Long.class, email);
            jdbc.update("delete from card where account_id in (select a.id from account a join customer c on c.id = a.customer_id where c.email = ?)", email);
            jdbc.update("delete from account where customer_id in (select id from customer where email = ?)", email);
            jdbc.update("delete from customer where email = ?", email);
            jdbc.update("delete from password_credential where id = ?", credentialId);
        }
    }
}
