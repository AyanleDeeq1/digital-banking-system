package io.github.ayanledeeq1.digitalbanking.exception;
public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException() { super("No debit card is available for this customer"); }
}
