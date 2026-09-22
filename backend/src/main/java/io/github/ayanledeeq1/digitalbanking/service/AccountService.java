package io.github.ayanledeeq1.digitalbanking.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import io.github.ayanledeeq1.digitalbanking.enums.AccountType;
import io.github.ayanledeeq1.digitalbanking.model.Account;
import io.github.ayanledeeq1.digitalbanking.repository.AccountRespository;

@Service 
public class AccountService {
    private  final AccountRespository accountRespository;
    private  final SecureRandom random = new SecureRandom();

    public  AccountService(AccountRespository accountRespository) {
        this.accountRespository = accountRespository;
    }

    public  Account saveAccount(Account account) {
        return  accountRespository.save(account);
    }

    public Account createAccount(String name, String accountNumber, AccountType accountType, AccountStatus status) {
        return  new Account(accountNumber, accountNumber, accountType, status);
    }


    public String generateAccountNumber() {
        String accountNumber = "";
        for (int i = 0; i < 10; i++) {
            int diggit = random.nextInt(10);
            accountNumber += diggit;
        }
        return  accountNumber;  
    }
    
}
