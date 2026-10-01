package io.github.ayanledeeq1.digitalbanking.dto.cardDto;
import java.time.LocalDate;

// No generated toString: the response contains simulated CVC2 data.
public class CardResponseDto {
    private final String cardNumber;
    private final String cardHolderName;
    private final LocalDate expiryDate;
    private final String cvc2;
    public CardResponseDto(String cardNumber, String cardHolderName, LocalDate expiryDate, String cvc2) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.expiryDate = expiryDate;
        this.cvc2 = cvc2;
    }
    public String getCardNumber() { return cardNumber; }
    public String getLastFour() { return cardNumber.substring(12); }
    public String getCardHolderName() { return cardHolderName; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getCvc2() { return cvc2; }
    public String getType() { return "DEBIT"; }
}
