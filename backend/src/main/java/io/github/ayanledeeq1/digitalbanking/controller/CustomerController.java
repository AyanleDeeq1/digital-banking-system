package io.github.ayanledeeq1.digitalbanking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ayanledeeq1.digitalbanking.dto.customerdto.CustomerRegisterResponseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.LoginRespnseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import io.github.ayanledeeq1.digitalbanking.service.CustomerService;
import jakarta.validation.Valid;

@RestController 
@RequestMapping("api/customers")
public class CustomerController {
    private  final CustomerService customerService;

    public  CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping()
    public ResponseEntity<CustomerRegisterResponseDto> registerCustomer(@Valid  @RequestBody RegisterCustomerDto registerCustomerDto ) {
        CustomerRegisterResponseDto responseDto = customerService.saveCustomer(registerCustomerDto);

        return  ResponseEntity
                  .status(HttpStatus.CREATED)
                  .body(responseDto);
    }

    @GetMapping("/me")
    public  ResponseEntity<LoginRespnseDto> getCurrentCustomer(Authentication authentication) {
        String email = authentication.getName();

        Customer customer = customerService.getCustomerByEmail(email); 
        System.out.println("custmer....................########################: " + customer.getFirstName());

        LoginRespnseDto respnseDto = new LoginRespnseDto(customer.getId(), customer.getFirstName(), customer.getLastNAme(), customer.getEmail());

        return  ResponseEntity.ok()
                    .body(respnseDto);
    }
    
}
