package io.github.ayanledeeq1.digitalbanking.dto.transactionDto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record AtmAmountRequestDto(@NotNull(message = "Amount is required") BigDecimal amount) {}
