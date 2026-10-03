package io.github.ayanledeeq1.digitalbanking.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import io.github.ayanledeeq1.digitalbanking.dto.transactionDto.*;
import io.github.ayanledeeq1.digitalbanking.enums.*;
import io.github.ayanledeeq1.digitalbanking.exception.CustomerNotFoundException;
import io.github.ayanledeeq1.digitalbanking.exception.TransferException;
import static io.github.ayanledeeq1.digitalbanking.exception.TransferException.Reason.*;
import io.github.ayanledeeq1.digitalbanking.model.*;
import io.github.ayanledeeq1.digitalbanking.repository.*;

@Service
public class TransferService {
    private final AccountRespository accounts;
    private final CustomerRepository customers;
    private final TransactionRepository transactions;
    private final LedgerEntryRepository ledger;
    private final AccountBalanceService balances;

    public TransferService(AccountRespository accounts, CustomerRepository customers,
            TransactionRepository transactions, LedgerEntryRepository ledger, AccountBalanceService balances) {
        this.accounts = accounts;
        this.customers = customers;
        this.transactions = transactions;
        this.ledger = ledger;
        this.balances = balances;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponseDto transfer(String email, Long sourceId, TransferRequestDto request) {
        BigDecimal amount;
        try {
            amount = MonetaryAmount.normalize(request.amount());
        } catch (IllegalArgumentException exception) {
            throw new TransferException(INVALID_TRANSFER,
                    "Amount must fit 17 integer digits and have at most two meaningful decimal places");
        }
        if (amount.signum() <= 0) {
            throw new TransferException(INVALID_TRANSFER, "Amount must be greater than zero");
        }
        if (request.destinationAccountNumber() == null || !request.destinationAccountNumber().matches("[0-9]{10}")) {
            throw new TransferException(INVALID_TRANSFER, "Enter a 10-digit URBank account number");
        }
        Customer customer = customers.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        if (!accounts.existsById(sourceId)) {
            throw new TransferException(ACCOUNT_NOT_FOUND, "Source account not found");
        }
        // V1 uses the generator's fixed URBank clearing prefix; storage remains unchanged.
        Long destinationId = accounts.findIdByAccountNumber("3424-5," + request.destinationAccountNumber())
                .orElseThrow(() -> new TransferException(ACCOUNT_NOT_FOUND, "Destination account not found"));
        if (sourceId.equals(destinationId)) {
            throw new TransferException(INVALID_TRANSFER, "Source and destination must be different accounts");
        }
        // Separate lock queries make acquisition order explicit, independent of database query plans.
        Account first = lock(Math.min(sourceId, destinationId));
        Account second = lock(Math.max(sourceId, destinationId));
        Account source = first.getId().equals(sourceId) ? first : second;
        Account destination = first.getId().equals(destinationId) ? first : second;
        requireOwner(source, customer);
        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException(INACTIVE_ACCOUNT, "Source account must be ACTIVE");
        }
        if (destination.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException(INACTIVE_ACCOUNT, "Destination account must be ACTIVE");
        }
        if (balances.getBalance(sourceId).compareTo(amount) < 0) {
            throw new TransferException(INSUFFICIENT_FUNDS, "Insufficient funds");
        }
        Transaction transaction;
        try {
            transaction = transactions.save(new Transaction(TransactionType.TRANSFER, amount,
                    Instant.now(), TransactionStatus.COMPLETED));
            ledger.saveAllAndFlush(List.of(new LedgerEntry(amount.negate(), source, transaction),
                    new LedgerEntry(amount, destination, transaction)));
        } catch (DataAccessException exception) {
            throw new TransferException(UNAVAILABLE, "Unable to complete the transfer. No money was moved");
        }
        return new TransactionResponseDto(transaction.getId(), transaction.getType(), transaction.getAmount(),
                transaction.getCreatedAt(), transaction.getStatus());
    }

    private Account lock(Long id) {
        try {
            return accounts.findByIdForUpdate(id)
                    .orElseThrow(() -> new TransferException(ACCOUNT_NOT_FOUND, "Account not found"));
        } catch (DataAccessException exception) {
            throw new TransferException(UNAVAILABLE, "Accounts are busy. Please try again shortly");
        }
    }

    private void requireOwner(Account source, Customer customer) {
        if (!source.getCustomer().getId().equals(customer.getId())) {
            throw new TransferException(NOT_OWNER, "Source account does not belong to you");
        }
    }
}
