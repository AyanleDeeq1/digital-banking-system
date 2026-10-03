package io.github.ayanledeeq1.digitalbanking.exception;

public class AtmException extends RuntimeException {
    public enum Reason { PIN_REQUIRED, INCORRECT_PIN, ACCOUNT_NOT_FOUND, NOT_OWNER, INACTIVE_ACCOUNT,
        INVALID_AMOUNT, INSUFFICIENT_FUNDS, UNAVAILABLE }
    private final Reason reason;
    public AtmException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }
    public Reason getReason() { return reason; }
}
