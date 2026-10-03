package io.github.ayanledeeq1.digitalbanking.exception;

public class TransferException extends RuntimeException {
    public enum Reason { ACCOUNT_NOT_FOUND, NOT_OWNER, INACTIVE_ACCOUNT, INVALID_TRANSFER, INSUFFICIENT_FUNDS, UNAVAILABLE }
    private final Reason reason;

    public TransferException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
