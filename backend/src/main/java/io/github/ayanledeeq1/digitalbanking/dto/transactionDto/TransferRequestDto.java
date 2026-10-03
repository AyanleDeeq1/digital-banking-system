package io.github.ayanledeeq1.digitalbanking.dto.transactionDto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TransferRequestDto(
        @NotNull @Pattern(regexp = "[0-9]{10}", message = "Enter a 10-digit URBank account number")
        String destinationAccountNumber,
        @NotNull(message = "Amount is required") BigDecimal amount) {}
