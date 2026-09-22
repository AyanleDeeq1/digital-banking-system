package io.github.ayanledeeq1.digitalbanking.service.Costumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.ayanledeeq1.digitalbanking.dto.customerdto.CustomerRegisterResponseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto;
import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import io.github.ayanledeeq1.digitalbanking.exception.CustomerNotFoundException;
import io.github.ayanledeeq1.digitalbanking.model.Account;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import io.github.ayanledeeq1.digitalbanking.model.PasswordCredential;
import io.github.ayanledeeq1.digitalbanking.repository.CustomerRepository;
import io.github.ayanledeeq1.digitalbanking.service.AccountService;
import io.github.ayanledeeq1.digitalbanking.service.CustomerService;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {
    @Mock 
    AccountService accountService;
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



}
