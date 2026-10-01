package io.github.ayanledeeq1.digitalbanking.exception;
public class CardIssuanceException extends RuntimeException {
    public CardIssuanceException() { super("Unable to issue a debit card. Registration was not completed"); }
}
