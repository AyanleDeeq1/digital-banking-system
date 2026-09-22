package io.github.ayanledeeq1.digitalbanking.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import io.github.ayanledeeq1.digitalbanking.model.Account;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import io.github.ayanledeeq1.digitalbanking.repository.CustomerRepository;
import io.github.ayanledeeq1.digitalbanking.service.CustomerService;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@SpringBootTest
@AutoConfigureMockMvc()
@Transactional
@ActiveProfiles("test")
public class customerIntegrationTest {
    @Autowired 
    MockMvc mockMvc;
    @Autowired 
    CustomerRepository customerRepository;

    @Test 
    void  saveCustomerIntegrationTest() throws Exception{
        mockMvc.perform(post("/api/customers")
                  .with(csrf())
                 .contentType(MediaType.APPLICATION_JSON)
                 .content("""
                            {
                                "firstName": "aye",
                                "lastName": "deeq",
                                "email": "aye@gmail.com",
                                "password": "Aye"
                            }
                         """)
            )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("aye"))
            .andExpect(jsonPath("$.email").value("aye@gmail.com"));
        
        Customer customer = customerRepository.findByEmail("aye@gmail.com").orElseThrow();

        Account account = customer.getAccounts().get(0);

        assertEquals("aye", customer.getFirstName());
        assertEquals(1,  customer.getAccounts().size());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }
}
