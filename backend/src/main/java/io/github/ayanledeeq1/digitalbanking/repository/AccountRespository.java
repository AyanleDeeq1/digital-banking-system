package io.github.ayanledeeq1.digitalbanking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ayanledeeq1.digitalbanking.model.Account;

public interface AccountRespository extends  JpaRepository<Account, Long>{
    
    @Query ("SELECT account FROM Account account WHERE account.accountNumber = :accountNumber")
    public  Optional<Account> findAccountByNumber(@Param("accountNumber") String accountNumber);
}
