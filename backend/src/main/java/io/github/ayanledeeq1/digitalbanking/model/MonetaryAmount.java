package io.github.ayanledeeq1.digitalbanking.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Shared persistence invariant for DECIMAL(19,2), not a business spending limit.
final class MonetaryAmount {
    private MonetaryAmount() {}

    static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        BigDecimal normalized;
        try {
            normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount must not have non-zero digits beyond two decimal places", exception);
        }
        if (normalized.precision() > 19) {
            throw new IllegalArgumentException("Amount exceeds DECIMAL(19,2) storage precision");
        }
        return normalized;
    }
}
