package io.github.ayanledeeq1.digitalbanking.service.Costumer.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.SecurityContextRepository;

import io.github.ayanledeeq1.digitalbanking.dto.customerdto.LoginRespnseDto;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.LogineRequestDto;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import io.github.ayanledeeq1.digitalbanking.model.PasswordCredential;
import io.github.ayanledeeq1.digitalbanking.service.AuthService;
import io.github.ayanledeeq1.digitalbanking.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock 
    CustomerService customerService;
    @Mock 
    AuthenticationManager authenticationManager;
    @Mock 
    SecurityContextRepository securityContextRepository;
     @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;
    @InjectMocks 
    AuthService authService;


    
    @Test 
    void  loginTest() {

        Authentication authentication = mock(Authentication.class);
        PasswordCredential credential = new PasswordCredential("hashedPassowrd");
        Customer customer = new Customer("aye", "deeq", "aye@gmail.com", credential);

        when((authenticationManager.authenticate(any(Authentication.class)))).thenReturn(authentication);

        when(authentication.getName()).thenReturn("aye@gmail.com");
        when(customerService.getCustomerByEmail(authentication.getName())).thenReturn(customer);

        LogineRequestDto requestDto = new LogineRequestDto("aye@gmail.com", "aye");


        LoginRespnseDto respnseDto = authService.login(requestDto, request, response);
        assertEquals("aye", respnseDto.firstName());
        assertEquals("aye@gmail.com", respnseDto.email());

        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository).saveContext(contextCaptor.capture(), eq(request), eq(response));

        SecurityContext capContext = contextCaptor.getValue();

        assertEquals(authentication, capContext.getAuthentication());
        
    }
}
