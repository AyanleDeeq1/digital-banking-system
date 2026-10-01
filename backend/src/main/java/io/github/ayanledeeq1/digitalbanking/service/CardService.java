package io.github.ayanledeeq1.digitalbanking.service;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import io.github.ayanledeeq1.digitalbanking.dto.cardDto.CardResponseDto;
import io.github.ayanledeeq1.digitalbanking.exception.CardIssuanceException;
import io.github.ayanledeeq1.digitalbanking.exception.CardNotFoundException;
import io.github.ayanledeeq1.digitalbanking.exception.CustomerNotFoundException;
import io.github.ayanledeeq1.digitalbanking.model.Account;
import io.github.ayanledeeq1.digitalbanking.model.Card;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import io.github.ayanledeeq1.digitalbanking.repository.CardRepository;
import io.github.ayanledeeq1.digitalbanking.repository.CustomerRepository;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final CustomerRepository customerRepository;
    private final CardDetailsGenerator generator;
    public CardService(CardRepository cardRepository, CustomerRepository customerRepository, CardDetailsGenerator generator) {
        this.cardRepository = cardRepository;
        this.customerRepository = customerRepository;
        this.generator = generator;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void issueCard(Customer customer, Account account) {
        try {
            for (int attempt = 0; attempt < 10; attempt++) {
                String number = generator.generateCardNumber();
                if (!cardRepository.existsByCardNumber(number)) {
                    Card card = new Card(number, generator.generateCvc2(), generator.expiryDate(), customer, account);
                    cardRepository.saveAndFlush(card);
                    return;
                }
            }
        } catch (DataAccessException exception) {
            // Duplicate-key messages can contain card numbers. Do not retain or log them.
            throw new CardIssuanceException();
        }
        throw new CardIssuanceException();
    }
    @Transactional(readOnly = true)
    public CardResponseDto getCurrentCard(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        Card card = cardRepository.findByCustomerEmail(customer.getEmail()).orElseThrow(CardNotFoundException::new);
        return new CardResponseDto(card.getCardNumber(),
                customer.getFirstName() + " " + customer.getLastNAme(), card.getExpiryDate(), card.getCvc2());
    }
}
