package io.github.ayanledeeq1.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@SpringBootTest 
@AutoConfigureMockMvc 
@Transactional 
@ActiveProfiles("test")
public class AuthIntegrationTest {
    @Autowired 
    MockMvc mockMvc;
    @BeforeEach 
    void  setUp() throws Exception {
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
        .andExpect(status().isCreated());
    }

    @Test 
    void logintTest() throws Exception{
        MvcResult result = mockMvc.perform((post("/api/customers/login"))
                      .with(csrf())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("""
                                {
                                    "email": "aye@gmail.com",
                                    "password": "Aye"
                                }
                              """)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

    MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

    SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    Authentication authentication = securityContext.getAuthentication();


    assertNotNull(session);
    assertNotNull(securityContext);
    assertNotNull(authentication);
    assertTrue(authentication.isAuthenticated());
    assertEquals("aye@gmail.com", authentication.getName());

            
    }
}
