package io.github.ayanledeeq1.digitalbanking.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ayanledeeq1.digitalbanking.model.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    @Query("""
            SELECT entry FROM LedgerEntry entry JOIN FETCH entry.account JOIN FETCH entry.transaction
            WHERE entry.account.id = :accountId AND entry.account.customer.email = :email
            ORDER BY entry.transaction.createdAt DESC, entry.transaction.id DESC, entry.id DESC
            """)
    List<LedgerEntry> findAccountHistory(@Param("accountId") Long accountId, @Param("email") String email);

    @Query("""
            SELECT entry FROM LedgerEntry entry JOIN FETCH entry.account JOIN FETCH entry.transaction
            WHERE entry.account.customer.email = :email
            ORDER BY entry.transaction.createdAt DESC, entry.transaction.id DESC, entry.id DESC
            """)
    List<LedgerEntry> findRecentHistory(@Param("email") String email, org.springframework.data.domain.Pageable pageable);

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
