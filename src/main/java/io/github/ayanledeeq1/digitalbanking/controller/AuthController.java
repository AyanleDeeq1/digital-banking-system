package io.github.ayanledeeq1.digitalbanking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ayanledeeq1.digitalbanking.dto.customerdto.LoginRespnseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.LogineRequestDto;
import io.github.ayanledeeq1.digitalbanking.service.AuthService;
import jakarta.validation.Valid;

@RestController 
@RequestMapping("api/customers")
public class AuthController {
    private  final AuthService authService;

    public  AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public  ResponseEntity<LoginRespnseDto> login(@Valid @RequestBody LogineRequestDto requestDto) {
        System.out.println("Hit  the Controller");
        LoginRespnseDto respnseDto = authService.login(requestDto);

        return  ResponseEntity.ok()
                   .body(respnseDto);
    }

    
}
