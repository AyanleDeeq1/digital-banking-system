package io.github.ayanledeeq1.digitalbanking.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import io.github.ayanledeeq1.digitalbanking.dto.customerdto.LoginRespnseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.LogineRequestDto;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service 
public class AuthService {
    private  final CustomerService customerService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(CustomerService customerService, AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository) {
        this.customerService = customerService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }


    public LoginRespnseDto login(LogineRequestDto requestDto, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPasssowrd())
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        securityContextRepository.saveContext(securityContext, request, response);

        Customer customer = customerService.getCustomerByEmail(authentication.getName());
        LoginRespnseDto respnseDto = new  LoginRespnseDto(
            customer.getId(), 
            customer.getFirstName(),
            customer.getLastNAme(),
            customer.getEmail()
        );

        return respnseDto;
    }

}
