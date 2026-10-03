package io.github.ayanledeeq1.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto;
import io.github.ayanledeeq1.digitalbanking.service.CustomerService;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class LogoutIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerService customers;

    private MockHttpSession login() throws Exception {
        customers.saveCustomer(new RegisterCustomerDto("Logout", "Test", "logout@example.com", "ValidPass1!"));
        return (MockHttpSession) mvc.perform(post("/api/customers/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"logout@example.com\",\"password\":\"ValidPass1!\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test void logoutInvalidatesSessionDeletesCookieAndRemovesAccountAccess() throws Exception {
        MockHttpSession session = login();
        assertNotNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        session.setAttribute("io.github.ayanledeeq1.digitalbanking.controller.AtmController.verifiedCard", 123L);
        mvc.perform(get("/api/customers/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/customers/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent()).andExpect(header().doesNotExist("Location"))
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/customers/me")).andExpect(status().isForbidden());
        mvc.perform(get("/api/customers/accounts")).andExpect(status().isForbidden());
        mvc.perform(get("/api/customers/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        mvc.perform(post("/api/customers/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"logout@example.com\",\"password\":\"ValidPass1!\"}"))
                .andExpect(status().isOk());
    }

    @Test void missingOrInvalidCsrfDoesNotLogCustomerOut() throws Exception {
        MockHttpSession session = login();
        mvc.perform(post("/api/customers/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/customers/logout").session(session).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        assertFalse(session.isInvalid());
        mvc.perform(get("/api/customers/me").session(session)).andExpect(status().isOk());
    }

    @Test void getRequestCannotLogCustomerOut() throws Exception {
        MockHttpSession session = login();
        mvc.perform(get("/api/customers/logout").session(session)).andExpect(status().is4xxClientError());
        assertFalse(session.isInvalid());
        mvc.perform(get("/api/customers/me").session(session)).andExpect(status().isOk());
    }
}
