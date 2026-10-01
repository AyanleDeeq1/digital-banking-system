package io.github.ayanledeeq1.digitalbanking.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ayanledeeq1.digitalbanking.model.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    @Query("SELECT SUM(entry.amount) FROM LedgerEntry entry WHERE entry.account.id = :accountId")
    BigDecimal sumAmountsByAccountId(@Param("accountId") Long accountId);

    @Query("""
            SELECT entry.account.id AS accountId, SUM(entry.amount) AS balance
            FROM LedgerEntry entry
            WHERE entry.account.id IN :accountIds
            GROUP BY entry.account.id
            """)
    List<AccountBalance> sumAmountsByAccountIds(@Param("accountIds") Collection<Long> accountIds);

    interface AccountBalance {
        Long getAccountId();
        BigDecimal getBalance();
    }
}
