package io.github.ayanledeeq1.digitalbanking.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/customers/")
public class CsrfController {

    @GetMapping("csrf")
    public CsrfToken getCsrfToken(CsrfToken csrfToken) {
        return  csrfToken;
    }
}
