package io.github.ayanledeeq1.digitalbanking.dto.accountDto;

import io.github.ayanledeeq1.digitalbanking.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AccountCreateDto {
    
    @NotBlank(message = "Account Name is required")
    private  String name;
    @NotNull
    private AccountType accountType;

    public AccountCreateDto(String name, AccountType accountType) {
        this.name = name;
        this.accountType = accountType;
    }

    public  String getName() {
        return  name;
    }

    public  AccountType getAccountType() {
        return  accountType;
    }
    
}
