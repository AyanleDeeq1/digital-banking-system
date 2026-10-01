package io.github.ayanledeeq1.digitalbanking.dto.accountDto;

import java.math.BigDecimal;

import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import io.github.ayanledeeq1.digitalbanking.enums.AccountType;

public class AccountResponseDto {
    private Long id;
    private  String name;
    private  String accountNumber;
    private  AccountType accountType;
    private  AccountStatus status;
    private BigDecimal balance;


    public  AccountResponseDto(Long id, String name, String accountNumber, AccountType accountType, AccountStatus status, BigDecimal balance) {
        this.id = id;
        this.name = name;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.status = status;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public  String getName() {
        return  name;
    }

    public  String getAccountNumber() {
        return  accountNumber;
    }

    public  AccountType getType() {
        return  accountType;
    }

    public  AccountStatus getStatus() {
        return  status;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
