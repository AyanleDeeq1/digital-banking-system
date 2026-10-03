package io.github.ayanledeeq1.digitalbanking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import io.github.ayanledeeq1.digitalbanking.dto.cardDto.AtmPinRequestDto;
import io.github.ayanledeeq1.digitalbanking.dto.transactionDto.*;
import io.github.ayanledeeq1.digitalbanking.service.AtmService;

@RestController
@RequestMapping("/api/customers/atm")
public class AtmController {
    private static final String VERIFIED_CARD = AtmController.class.getName() + ".verifiedCard";
    private final AtmService atm;
    public AtmController(AtmService atm) { this.atm = atm; }

    @PostMapping("/pin")
    public ResponseEntity<Void> verify(@Valid @RequestBody AtmPinRequestDto request,
            Authentication authentication, HttpSession session) {
        session.removeAttribute(VERIFIED_CARD);
        session.setAttribute(VERIFIED_CARD, atm.verifyPin(authentication.getName(), request.pin()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/eject")
    public ResponseEntity<Void> eject(HttpSession session) {
        session.removeAttribute(VERIFIED_CARD);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accounts/{accountId}/deposits")
    public ResponseEntity<TransactionResponseDto> deposit(@PathVariable Long accountId,
            @Valid @RequestBody AtmAmountRequestDto request, Authentication authentication, HttpSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atm.deposit(authentication.getName(),
                (Long) session.getAttribute(VERIFIED_CARD), accountId, request.amount()));
    }

    @PostMapping("/accounts/{accountId}/withdrawals")
    public ResponseEntity<TransactionResponseDto> withdraw(@PathVariable Long accountId,
            @Valid @RequestBody AtmAmountRequestDto request, Authentication authentication, HttpSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atm.withdraw(authentication.getName(),
                (Long) session.getAttribute(VERIFIED_CARD), accountId, request.amount()));
    }
}
