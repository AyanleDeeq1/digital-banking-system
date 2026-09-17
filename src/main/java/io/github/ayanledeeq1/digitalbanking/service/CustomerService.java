package io.github.ayanledeeq1.digitalbanking.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.github.ayanledeeq1.digitalbanking.dto.customerdto.CustomerRegisterResponseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import io.github.ayanledeeq1.digitalbanking.model.PasswordCredential;
import io.github.ayanledeeq1.digitalbanking.repository.CustomerRepository;

@Service 
public class CustomerService {
    private  final CustomerRepository customerRepository;
    private  final PasswordEncoder passwordEncoder;

    public  CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }


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

        Customer savedcustomer = customerRepository.save(customer); //persiste the customer and passowrd credentail

        // creeate response dto object
        CustomerRegisterResponseDto responseDto = new CustomerRegisterResponseDto(
            savedcustomer.getId(),
            savedcustomer.getFirstName(),
             savedcustomer.getLastNAme(), 
             savedcustomer.getEmail()
            );

        return  responseDto;
    }
    
}
