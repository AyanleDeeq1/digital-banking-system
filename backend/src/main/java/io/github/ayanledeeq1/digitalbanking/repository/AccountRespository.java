package io.github.ayanledeeq1.digitalbanking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ayanledeeq1.digitalbanking.model.Account;

public interface AccountRespository extends  JpaRepository<Account, Long>{
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM Account account WHERE account.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT account.id FROM Account account WHERE account.accountNumber = :accountNumber")
    Optional<Long> findIdByAccountNumber(@Param("accountNumber") String accountNumber);
    
    @Query ("SELECT account FROM Account account WHERE account.accountNumber = :accountNumber")
    public  Optional<Account> findAccountByNumber(@Param("accountNumber") String accountNumber);
}
