package io.github.ayanledeeq1.digitalbanking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.github.ayanledeeq1.digitalbanking.dto.transactionDto.*;
import io.github.ayanledeeq1.digitalbanking.service.TransferService;

@RestController
@RequestMapping("/api/customers/accounts")
public class TransferController {
    private final TransferService transfers;

    public TransferController(TransferService transfers) { this.transfers = transfers; }

    @PostMapping("/{sourceAccountId}/transfers")
    public ResponseEntity<TransactionResponseDto> transfer(@PathVariable Long sourceAccountId,
            @Valid @RequestBody TransferRequestDto request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transfers.transfer(authentication.getName(), sourceAccountId, request));
    }
}
