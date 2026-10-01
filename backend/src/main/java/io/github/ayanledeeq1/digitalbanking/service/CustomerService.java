package io.github.ayanledeeq1.digitalbanking.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ayanledeeq1.digitalbanking.dto.accountDto.AccountCreateDto;
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

@Service 
public class CustomerService {
    private  final CustomerRepository customerRepository;
    private  final PasswordEncoder passwordEncoder;
    private  final AccountService accountService;
    private final AccountBalanceService accountBalanceService;

    public  CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder, AccountService accountService, AccountBalanceService accountBalanceService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
        this.accountBalanceService = accountBalanceService;
    }


    public  Customer getCustomerById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException("Coustmer with id " + id + " could not found"));
    }

      public  Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email).orElseThrow(() -> new CustomerNotFoundException("Coustmer with email " + email + " could not found"));
    }

    @Transactional 
    public CustomerRegisterResponseDto saveCustomer(RegisterCustomerDto requestdDto) {
        // hash password
        String hashedPassword = passwordEncoder.encode(requestdDto.getPassword());
        // create password object
        PasswordCredential passwordCredential = new PasswordCredential(hashedPassword);
        Customer customer = new Customer(
            requestdDto.getFirstName(),
            requestdDto.getLastName(),
            requestdDto.getEmail(),
            passwordCredential // password object
        );

        // Every new customer receives an active account on registration
        Account newAccount = new Account("Main Account", accountService.generateAccountNumber(), AccountType.CHECKING, AccountStatus.ACTIVE);

        customer.addAccount(newAccount);

        Customer savedcustomer = customerRepository.save(customer); //persiste the customer and passowrd credentail
        accountService.saveAccount(newAccount);

        // creeate response dto object
        CustomerRegisterResponseDto responseDto = new CustomerRegisterResponseDto(
            savedcustomer.getId(),
            savedcustomer.getFirstName(),
             savedcustomer.getLastNAme(), 
             savedcustomer.getEmail()
            );

        return  responseDto;
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> getCustomerAccounts(String email) {
        Customer customer = getCustomerByEmail(email);

        List<Account> accounts = customer.getAccounts();
        Map<Long, BigDecimal> balances = accountBalanceService.getBalances(
                accounts.stream().map(Account::getId).toList());
        
        List<AccountResponseDto> accountResponseDtoList = new ArrayList<>();
        for (Account account : accounts) {
            AccountResponseDto responseDto = new  AccountResponseDto(account.getId(), account.getAccountName(), account.getAccountNumber(), account.getType(), account.getStatus(), balances.get(account.getId()));
            accountResponseDtoList.add(responseDto);
        }
        return  accountResponseDtoList;
    }

    @Transactional 
    public  AccountResponseDto  createAnotherAccount(AccountCreateDto createDto, String email) {
        Customer customer = getCustomerByEmail(email);
        String accountNumber = accountService.generateAccountNumber();


       while (accountService.accountNumberExist(accountNumber)) {
        accountNumber = accountService.generateAccountNumber();
       }

        Account account = accountService.createAccount(createDto.getName(), accountNumber, createDto.getAccountType(), AccountStatus.ACTIVE);
        customer.addAccount(account);
        Account createdAccount = accountService.saveAccount(account);

        AccountResponseDto responseDto = new AccountResponseDto(
            createdAccount.getId(), 
            createdAccount.getAccountName(),
            createdAccount.getAccountNumber(), 
            createdAccount.getType(),
            createdAccount.getStatus(),
            accountBalanceService.getBalance(createdAccount.getId())
        );
        return  responseDto;

    }
    
}
