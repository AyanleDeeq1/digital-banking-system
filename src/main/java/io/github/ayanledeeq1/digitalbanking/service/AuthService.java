package io.github.ayanledeeq1.digitalbanking.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import io.github.ayanledeeq1.digitalbanking.dto.customerdto.LoginRespnseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.LogineRequestDto;
import io.github.ayanledeeq1.digitalbanking.model.Customer;

@Service 
public class AuthService {
    private  final CustomerService customerService;
    private final AuthenticationManager authenticationManager;

    public AuthService(CustomerService customerService, AuthenticationManager authenticationManager) {
        this.customerService = customerService;
        this.authenticationManager = authenticationManager;
    }


    public LoginRespnseDto login(LogineRequestDto requestDto) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPasssowrd())
        );

        System.out.println("Authentucaton.....................: " + authentication.getName());

        Customer customer = customerService.getCosutmerByEmail(authentication.getName());
        LoginRespnseDto respnseDto = new  LoginRespnseDto(
            customer.getId(), 
            customer.getFirstName(),
            customer.getLastNAme(),
            customer.getEmail()
        );

        return respnseDto;
    }

}
