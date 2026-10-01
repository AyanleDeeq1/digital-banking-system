package io.github.ayanledeeq1.digitalbanking.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ayanledeeq1.digitalbanking.repository.LedgerEntryRepository;

@Service
@Transactional(readOnly = true)
public class AccountBalanceService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountBalanceService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    // Internal calculation only: callers must resolve and authorize accounts.
    public BigDecimal getBalance(Long accountId) {
        BigDecimal balance = ledgerEntryRepository.sumAmountsByAccountId(accountId);
        return balance == null ? ZERO : balance;
    }

    public Map<Long, BigDecimal> getBalances(Collection<Long> accountIds) {
        Map<Long, BigDecimal> balances = new HashMap<>();
        for (Long accountId : accountIds) {
            balances.put(accountId, ZERO);
        }
        if (!accountIds.isEmpty()) {
            for (LedgerEntryRepository.AccountBalance balance : ledgerEntryRepository.sumAmountsByAccountIds(accountIds)) {
                balances.put(balance.getAccountId(), balance.getBalance());
            }
        }
        return balances;
    }
}
