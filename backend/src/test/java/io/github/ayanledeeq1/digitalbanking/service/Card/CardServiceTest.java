package io.github.ayanledeeq1.digitalbanking.service.Card;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import io.github.ayanledeeq1.digitalbanking.enums.*;
import io.github.ayanledeeq1.digitalbanking.model.*;
import io.github.ayanledeeq1.digitalbanking.repository.*;
import io.github.ayanledeeq1.digitalbanking.service.*;
import io.github.ayanledeeq1.digitalbanking.exception.CardIssuanceException;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {
    @Mock CardRepository repository;
    @Mock CustomerRepository customers;
    @Mock CardDetailsGenerator generator;
    @InjectMocks CardService service;
    Customer customer;
    Account account;
    @BeforeEach void setup() {
        customer = new Customer("Test", "Owner", "owner@example.com", new PasswordCredential("hash"));
        ReflectionTestUtils.setField(customer, "id", 1L);
        account = new Account("Main Account", "3424-5,1234567890", AccountType.CHECKING, AccountStatus.ACTIVE);
        customer.addAccount(account);
    }
    @Test void retriesKnownCollisionBeforeSaving() {
        when(generator.generateCardNumber()).thenReturn("0000000000000001", "0000000000000002");
        when(repository.existsByCardNumber("0000000000000001")).thenReturn(true);
        when(generator.generateCvc2()).thenReturn("007");
        when(generator.expiryDate()).thenReturn(LocalDate.of(2029, 10, 1));
        service.issueCard(customer, account);
        ArgumentCaptor<Card> card = ArgumentCaptor.forClass(Card.class);
        verify(repository).saveAndFlush(card.capture());
        assertEquals("0000000000000002", card.getValue().getCardNumber());
        assertEquals("007", card.getValue().getCvc2());
        assertSame(account, card.getValue().getAccount());
        assertSame(customer, card.getValue().getCustomer());
    }
    @Test void stopsAfterTenCollisions() {
        when(generator.generateCardNumber()).thenReturn("0000000000000001");
        when(repository.existsByCardNumber(anyString())).thenReturn(true);
        assertThrows(CardIssuanceException.class, () -> service.issueCard(customer, account));
        verify(generator, times(10)).generateCardNumber();
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void databaseRaceFailsWithoutRetryOrSensitiveExceptionText() {
        when(generator.generateCardNumber()).thenReturn("0000000000000001");
        when(generator.generateCvc2()).thenReturn("007");
        when(generator.expiryDate()).thenReturn(LocalDate.of(2029, 10, 1));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("sensitive database detail"));
        CardIssuanceException failure = assertThrows(CardIssuanceException.class, () -> service.issueCard(customer, account));
        assertFalse(failure.getMessage().contains("sensitive"));
        assertNull(failure.getCause());
        verify(generator).generateCardNumber();
    }
    @Test void cardRejectsMismatchedOwnerAndInvalidIdentifiers() {
        Customer other = new Customer("Other", "Owner", "other@example.com", new PasswordCredential("hash"));
        ReflectionTestUtils.setField(other, "id", 2L);
        LocalDate expiry = LocalDate.of(2029, 10, 1);
        assertThrows(IllegalArgumentException.class, () -> new Card("0000000000000001", "007", expiry, other, account));
        assertThrows(IllegalArgumentException.class, () -> new Card("123", "007", expiry, customer, account));
        assertThrows(IllegalArgumentException.class, () -> new Card("0000000000000001", "7", expiry, customer, account));
    }
}
