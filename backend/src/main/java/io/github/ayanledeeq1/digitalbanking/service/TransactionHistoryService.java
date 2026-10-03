package io.github.ayanledeeq1.digitalbanking.service;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.ayanledeeq1.digitalbanking.dto.transactionDto.TransactionHistoryDto;
import io.github.ayanledeeq1.digitalbanking.exception.AtmException;
import io.github.ayanledeeq1.digitalbanking.model.LedgerEntry;
import io.github.ayanledeeq1.digitalbanking.repository.AccountRespository;
import io.github.ayanledeeq1.digitalbanking.repository.LedgerEntryRepository;

@Service
@Transactional(readOnly = true)
public class TransactionHistoryService {
    private final AccountRespository accounts;
    private final LedgerEntryRepository ledger;

    public TransactionHistoryService(AccountRespository accounts, LedgerEntryRepository ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    public List<TransactionHistoryDto> accountHistory(String email, Long accountId) {
        var account = accounts.findById(accountId).orElseThrow(() ->
                new AtmException(AtmException.Reason.ACCOUNT_NOT_FOUND, "Account not found"));
        if (!account.getCustomer().getEmail().equals(email)) {
            throw new AtmException(AtmException.Reason.NOT_OWNER, "Account does not belong to you");
        }
        return ledger.findAccountHistory(accountId, email).stream().map(this::response).toList();
    }

    public List<TransactionHistoryDto> recentHistory(String email) {
        return ledger.findRecentHistory(email, PageRequest.of(0, 5)).stream().map(this::response).toList();
    }

    private TransactionHistoryDto response(LedgerEntry entry) {
        var transaction = entry.getTransaction();
        return new TransactionHistoryDto(entry.getId(), transaction.getId(), entry.getAccount().getId(),
                entry.getAccount().getAccountName(), transaction.getType(), entry.getAmount(),
                transaction.getCreatedAt(), transaction.getStatus());
    }
}
