package io.github.ayanledeeq1.digitalbanking.service.Account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import io.github.ayanledeeq1.digitalbanking.model.Account;
import io.github.ayanledeeq1.digitalbanking.repository.AccountRespository;
import io.github.ayanledeeq1.digitalbanking.service.AccountService;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
    @Mock
    AccountRespository accountRespository;
    
    @InjectMocks 
    AccountService accountService;

    @Test
    void gernerateAccountnumberTest() {
        String accountNumber = accountService.generateAccountNumber();

        assertTrue(accountNumber.matches("\\d{10}"));
    }

    @Test
    void  createAccount() {
        String acountNumber = "122sfef989";

        Account account = accountService.createAccount(acountNumber, AccountStatus.ACTIVE);

        assertEquals("122sfef989", account.getAccountNumber());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());

    }

    @Test 
    void  saveAccountTest() {
        String acountNumber = "122sfef989";

        Account account = accountService.createAccount(acountNumber, AccountStatus.CLOSED);

        when(accountRespository.save(account)).thenReturn(account);
        Account savedAccount = accountService.saveAccount(account);

        assertEquals("122sfef989", savedAccount.getAccountNumber());
        assertEquals(AccountStatus.CLOSED, savedAccount.getStatus());

        verify(accountRespository).save(account);

    }

}
