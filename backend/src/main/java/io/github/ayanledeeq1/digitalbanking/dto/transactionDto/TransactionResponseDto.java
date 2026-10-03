package io.github.ayanledeeq1.digitalbanking.dto.transactionDto;

import java.math.BigDecimal;
import java.time.Instant;
import io.github.ayanledeeq1.digitalbanking.enums.TransactionType;
import io.github.ayanledeeq1.digitalbanking.enums.TransactionStatus;

public record TransactionResponseDto(Long id, TransactionType type, BigDecimal amount,
        Instant createdAt, TransactionStatus status) {}
