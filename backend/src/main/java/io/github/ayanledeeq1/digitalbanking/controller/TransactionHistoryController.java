package io.github.ayanledeeq1.digitalbanking.controller;

import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.github.ayanledeeq1.digitalbanking.dto.transactionDto.TransactionHistoryDto;
import io.github.ayanledeeq1.digitalbanking.service.TransactionHistoryService;

@RestController
@RequestMapping("/api/customers")
public class TransactionHistoryController {
    private final TransactionHistoryService history;

    public TransactionHistoryController(TransactionHistoryService history) { this.history = history; }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionHistoryDto>> account(@PathVariable Long accountId, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(history.accountHistory(authentication.getName(), accountId));
    }

    @GetMapping("/transactions/recent")
    public ResponseEntity<List<TransactionHistoryDto>> recent(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(history.recentHistory(authentication.getName()));
    }
}
