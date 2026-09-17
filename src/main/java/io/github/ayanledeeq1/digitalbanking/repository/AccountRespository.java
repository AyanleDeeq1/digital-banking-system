package io.github.ayanledeeq1.digitalbanking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ayanledeeq1.digitalbanking.model.Account;

public interface AccountRespository extends  JpaRepository<Account, Long>{
    
    
}
