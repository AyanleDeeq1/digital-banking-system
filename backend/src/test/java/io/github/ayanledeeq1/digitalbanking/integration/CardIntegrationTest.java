package io.github.ayanledeeq1.digitalbanking.integration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto;
import io.github.ayanledeeq1.digitalbanking.dto.accountDto.AccountCreateDto;
import io.github.ayanledeeq1.digitalbanking.enums.*;
import io.github.ayanledeeq1.digitalbanking.model.*;
import io.github.ayanledeeq1.digitalbanking.repository.*;
import io.github.ayanledeeq1.digitalbanking.service.*;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CardIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerService customers;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRespository accounts;
    @Autowired CardRepository cards;
    @Autowired EntityManager em;
    @Autowired CardPinCipher pinCipher;
    @MockitoBean CardDetailsGenerator generator;

    @BeforeEach void setup() {
        when(generator.generateCardNumber()).thenReturn("0000000000000001", "0000000000000002");
        when(generator.generateCvc2()).thenReturn("007");
        when(generator.generatePin()).thenReturn("0123");
        when(generator.expiryDate()).thenReturn(LocalDate.of(2029, 10, 1));
    }
    private void register(String email) {
        customers.saveCustomer(new RegisterCustomerDto("Card", "Owner", email, "test-password"));
    }
    @Test void registrationCreatesOneCardAndAdditionalAccountsHaveNoCard() {
        register("card@example.com");
        Customer customer = customerRepository.findByEmail("card@example.com").orElseThrow();
        Long initialAccount = customer.getAccounts().getFirst().getId();
        var extra = customers.createAnotherAccount(new AccountCreateDto("Extra checking", AccountType.CHECKING), customer.getEmail());
        em.flush();
        em.clear();
        Card card = cards.findFirstByAccountCustomerEmailOrderByAccountIdAsc("card@example.com").orElseThrow();
        assertEquals(1, cards.count());
        assertEquals(customer.getId(), card.getAccount().getCustomer().getId());
        assertEquals(initialAccount, card.getAccount().getId());
        assertEquals("0000000000000001", card.getCardNumber());
        assertEquals("007", card.getCvc2());
        assertNotEquals("0123", card.getEncryptedPin());
        assertTrue(pinCipher.matches("0123", card.getEncryptedPin()));
        assertEquals(LocalDate.of(2029, 10, 1), card.getExpiryDate());
        assertTrue(cards.findByAccountId(extra.getId()).isEmpty());
    }
    @Test void endpointReturnsOnlyOwnFullCardAndPreservesLeadingZeros() throws Exception {
        register("first@example.com");
        register("second@example.com");
        em.flush();
        em.clear();
        mvc.perform(get("/api/customers/card").with(user("first@example.com")).param("customerId", "999"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.lastFour").value("0001"))
                .andExpect(jsonPath("$.cardHolderName").value("Card Owner"))
                .andExpect(jsonPath("$.expiryDate").value("2029-10-01"))
                .andExpect(jsonPath("$.cvc2").value("007"))
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andExpect(jsonPath("$.cardNumber").value("0000000000000001"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.pin").doesNotExist())
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andExpect(jsonPath("$.accountId").doesNotExist());
        mvc.perform(get("/api/customers/card").with(user("second@example.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lastFour").value("0002"))
                .andExpect(jsonPath("$.cardNumber").value("0000000000000002"));
        assertEquals(2, cards.count()); // Same CVC2 is valid on different cards.
    }
    @Test void endpointKeepsRegistrationCardAfterAdditionalAccountCreation() throws Exception {
        register("multiple-cards@example.com");
        var extra = customers.createAnotherAccount(new AccountCreateDto("Extra", AccountType.SAVINGS), "multiple-cards@example.com");
        em.flush();
        em.clear();
        mvc.perform(get("/api/customers/card").with(user("multiple-cards@example.com"))
                .param("accountId", extra.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("0000000000000001"));
        assertTrue(cards.findByAccountId(extra.getId()).isEmpty());
        assertEquals(1, cards.count());
    }
    @Test void missingCardUsesExistingErrorShape() throws Exception {
        customerRepository.save(new Customer("Old", "Customer", "old@example.com", new PasswordCredential("hash")));
        mvc.perform(get("/api/customers/card").with(user("old@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.massage").value("No debit card is available for this customer"));
    }
    @Test void unauthenticatedAccessIsRejected() throws Exception {
        mvc.perform(get("/api/customers/card")).andExpect(status().isForbidden());
    }
    private Card extraCard(String number, Account account) {
        return new Card(number, "007", LocalDate.of(2029, 10, 1), pinCipher.encrypt("0123"), account);
    }
    @Test void databaseRejectsDuplicateCardNumber() {
        register("first@example.com");
        register("second@example.com");
        Card second = cards.findFirstByAccountCustomerEmailOrderByAccountIdAsc("second@example.com").orElseThrow();
        cards.delete(second);
        cards.flush();
        assertThrows(DataIntegrityViolationException.class, () ->
                cards.saveAndFlush(extraCard("0000000000000001", second.getAccount())));
    }
    @Test void databaseRejectsSecondCardForSameAccount() {
        register("first@example.com");
        Card first = cards.findFirstByAccountCustomerEmailOrderByAccountIdAsc("first@example.com").orElseThrow();
        assertThrows(DataIntegrityViolationException.class, () ->
                cards.saveAndFlush(extraCard("0000000000000003", first.getAccount())));
    }
}
