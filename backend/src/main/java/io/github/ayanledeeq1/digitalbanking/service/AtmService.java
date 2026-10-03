package io.github.ayanledeeq1.digitalbanking.service;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import io.github.ayanledeeq1.digitalbanking.dto.transactionDto.TransactionResponseDto;
import io.github.ayanledeeq1.digitalbanking.enums.*;
import io.github.ayanledeeq1.digitalbanking.exception.*;
import static io.github.ayanledeeq1.digitalbanking.exception.AtmException.Reason.*;
import io.github.ayanledeeq1.digitalbanking.model.*;
import io.github.ayanledeeq1.digitalbanking.repository.*;

@Service
public class AtmService {
    private final CardRepository cards;
    private final AccountRespository accounts;
    private final TransactionRepository transactions;
    private final LedgerEntryRepository ledger;
    private final AccountBalanceService balances;
    private final CardPinCipher cipher;

    public AtmService(CardRepository cards, AccountRespository accounts, TransactionRepository transactions,
            LedgerEntryRepository ledger, AccountBalanceService balances, CardPinCipher cipher) {
        this.cards = cards; 
        this.accounts = accounts; 
        this.transactions = transactions;
        this.ledger = ledger; 
        this.balances = balances; this.cipher = cipher;
    }

    @Transactional(readOnly = true)
    public Long verifyPin(String email, String pin) {
        Card card = cards.findByCustomerEmail(email).orElseThrow(CardNotFoundException::new);
        if (!cipher.matches(pin, card.getEncryptedPin())) {
            throw new AtmException(INCORRECT_PIN, "Incorrect PIN. Please try again");
        }
        return card.getId();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponseDto deposit(String email,
            Long verifiedCardId,
            Long accountId, 
            BigDecimal amount) {

        return move(email, verifiedCardId, accountId, amount, TransactionType.DEPOSIT);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponseDto withdraw(String email, Long verifiedCardId, Long accountId, BigDecimal amount) {
        return move(email, verifiedCardId, accountId, amount, TransactionType.WITHDRAWAL);
    }

    private TransactionResponseDto move(String email, Long verifiedCardId, Long accountId, BigDecimal requested,
            TransactionType type) {
        Card card = cards.findByCustomerEmail(email).orElseThrow(CardNotFoundException::new);
        if (verifiedCardId == null || !verifiedCardId.equals(card.getId())) {
            throw new AtmException(PIN_REQUIRED, "Insert your card and verify your PIN first");
        }
        BigDecimal amount;
        try { amount = MonetaryAmount.normalize(requested); }
        catch (IllegalArgumentException failure) {
            throw new AtmException(INVALID_AMOUNT, "Amount must fit 17 integer digits and have at most two meaningful decimal places");
        }
        if (amount.signum() <= 0) throw new AtmException(INVALID_AMOUNT, "Amount must be greater than zero");
        Account account;
        try {
            account = accounts.findByIdForUpdate(accountId)
                    .orElseThrow(() -> new AtmException(ACCOUNT_NOT_FOUND, "Account not found"));
        } catch (DataAccessException failure) {
            throw new AtmException(UNAVAILABLE, "Account is busy. Please try again shortly");
        }
        if (!account.getCustomer().getId().equals(card.getCustomer().getId())) {
            throw new AtmException(NOT_OWNER, "Account does not belong to you");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AtmException(INACTIVE_ACCOUNT, "Account must be ACTIVE");
        }
        if (type == TransactionType.WITHDRAWAL && balances.getBalance(accountId).compareTo(amount) < 0) {
            throw new AtmException(INSUFFICIENT_FUNDS, "Insufficient funds");
        }
        try {
            Transaction transaction = transactions.save(new Transaction(type, amount, Instant.now(), TransactionStatus.COMPLETED));
            ledger.saveAndFlush(new LedgerEntry(type == TransactionType.DEPOSIT ? amount : amount.negate(), account, transaction));
            return new TransactionResponseDto(transaction.getId(), transaction.getType(), transaction.getAmount(),
                    transaction.getCreatedAt(), transaction.getStatus());
        } catch (DataAccessException failure) {
            throw new AtmException(UNAVAILABLE, "Unable to complete the operation. No money was moved");
        }
    }
}
