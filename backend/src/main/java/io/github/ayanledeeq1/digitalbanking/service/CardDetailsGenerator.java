package io.github.ayanledeeq1.digitalbanking.service;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class CardDetailsGenerator {
    private final SecureRandom random;
    private final Clock clock;
    public CardDetailsGenerator(@Qualifier("cardRandom") SecureRandom random, @Qualifier("cardClock") Clock clock) {
        this.random = random;
        this.clock = clock;
    }
    public String generateCardNumber() { return digits(16); }
    public String generateCvc2() { return digits(3); }
    public LocalDate expiryDate() { return LocalDate.now(clock).plusYears(3); }
    private String digits(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) result.append(random.nextInt(10));
        return result.toString();
    }
}
