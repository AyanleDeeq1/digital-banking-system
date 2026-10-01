package io.github.ayanledeeq1.digitalbanking.service.Costumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.ayanledeeq1.digitalbanking.dto.accountDto.AccountResponseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.CustomerRegisterResponseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto;
import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import io.github.ayanledeeq1.digitalbanking.enums.AccountType;
import io.github.ayanledeeq1.digitalbanking.exception.CustomerNotFoundException;
import io.github.ayanledeeq1.digitalbanking.model.Account;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import io.github.ayanledeeq1.digitalbanking.model.PasswordCredential;
import io.github.ayanledeeq1.digitalbanking.repository.CustomerRepository;
import io.github.ayanledeeq1.digitalbanking.service.AccountService;
import io.github.ayanledeeq1.digitalbanking.service.AccountBalanceService;
import io.github.ayanledeeq1.digitalbanking.service.CustomerService;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {
    @Mock 
    AccountService accountService;
    @Mock
    AccountBalanceService accountBalanceService;
    @Mock 
    CustomerRepository customerRepository;
    @Mock 
    PasswordEncoder passwordEncoder;
    @InjectMocks 
    CustomerService customerService;

    @Test 
    void  saveCustomerAndFirstAccountTest() {
        RegisterCustomerDto regsiterDto = new RegisterCustomerDto("aye", "Deeq", "aye@gmail.com", "aye");


        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("aye")).thenReturn("hashedPassword");
        when(accountService.generateAccountNumber()).thenReturn("123456767");
        
        CustomerRegisterResponseDto responseDto = customerService.saveCustomer(regsiterDto);


        assertEquals("aye", responseDto.getFirstName());
        assertEquals("aye@gmail.com", responseDto.getEmail());
        
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

        verify(accountService).saveAccount(accountCaptor.capture());

        Account captureAccount = accountCaptor.getValue();

        assertEquals("123456767", captureAccount.getAccountNumber());
        assertEquals(AccountStatus.ACTIVE, captureAccount.getStatus());
    }

    @Test
    void getCutomerById() {
        PasswordCredential passwordCredential = new PasswordCredential("hashedpassowed");
        Customer customer = new Customer("aye", "deeq", "aye@gmail.com", passwordCredential);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Customer c = customerService.getCustomerById(1L);

        assertEquals("aye", c.getFirstName());
    }

     @Test
    void getCutomerByEmail() {
        PasswordCredential passwordCredential = new PasswordCredential("hashedpassowed");
        Customer customer = new Customer("aye", "deeq", "aye@gmail.com", passwordCredential);
        when(customerRepository.findByEmail("aye@gmail.com")).thenReturn(Optional.of(customer));

        Customer c = customerService.getCustomerByEmail("aye@gmail.com");

        assertEquals("aye", c.getFirstName());
    }

    @Test 
    void CustomerNotFoundTest() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        when(customerRepository.findByEmail("ali@gmail.com")).thenReturn(Optional.empty());
        assertThrows(CustomerNotFoundException.class, () ->  customerService.getCustomerById(99L));
        assertThrows(CustomerNotFoundException.class, () ->  customerService.getCustomerByEmail("ali@gmail.com"));
    }

    @Test
    void accountListUsesOneGroupedBalanceCalculation() {
        Customer customer = new Customer("aye", "deeq", "aye@gmail.com", new PasswordCredential("hash"));
        Account first = new Account("Checking", "3424-5,0000000001", AccountType.CHECKING, AccountStatus.ACTIVE);
        Account second = new Account("Savings", "3424-5,0000000002", AccountType.SAVINGS, AccountStatus.ACTIVE);
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        customer.addAccount(first);
        customer.addAccount(second);
        when(customerRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(accountBalanceService.getBalances(List.of(1L, 2L)))
                .thenReturn(Map.of(1L, new BigDecimal("70.15"), 2L, new BigDecimal("0.00")));

        List<AccountResponseDto> accounts = customerService.getCustomerAccounts(customer.getEmail());

        assertEquals(2, accounts.size());
        assertEquals(new BigDecimal("70.15"), accounts.get(0).getBalance());
        assertEquals(new BigDecimal("0.00"), accounts.get(1).getBalance());
        verify(accountBalanceService).getBalances(List.of(1L, 2L));
        verifyNoMoreInteractions(accountBalanceService);
    }



}
