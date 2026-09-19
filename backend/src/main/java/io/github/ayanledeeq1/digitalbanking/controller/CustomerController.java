package io.github.ayanledeeq1.digitalbanking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ayanledeeq1.digitalbanking.dto.customerdto.CustomerRegisterResponseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto;
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
    
}
