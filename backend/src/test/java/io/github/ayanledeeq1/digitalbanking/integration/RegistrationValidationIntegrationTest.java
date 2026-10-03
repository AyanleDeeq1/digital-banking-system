package io.github.ayanledeeq1.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationValidationIntegrationTest {
    private static final String EMAIL = "registration-validation@example.com";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;

    private ResultActions register(String firstName, String email, String password) throws Exception {
        return mvc.perform(post("/api/customers").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("firstName", firstName, "lastName", "Test",
                        "email", email, "password", password))));
    }

    private List<Long> counts() {
        return List.of("customer", "password_credential", "account", "card").stream()
                .map(table -> jdbc.queryForObject("select count(*) from " + table, Long.class)).toList();
    }

    @AfterEach void cleanup() {
        var credentials = jdbc.queryForList("select password_credential_id from customer where email = ?", Long.class, EMAIL);
        jdbc.update("delete from card where customer_id in (select id from customer where email = ?)", EMAIL);
        jdbc.update("delete from account where customer_id in (select id from customer where email = ?)", EMAIL);
        jdbc.update("delete from customer where email = ?", EMAIL);
        credentials.forEach(id -> jdbc.update("delete from password_credential where id = ?", id));
    }

    @ParameterizedTest
    @CsvSource({
        "Aa1!abc, At least 8 characters",
        "abcdef1!, One uppercase letter",
        "ABCDEF1!, One lowercase letter",
        "Abcdefg!, One number",
        "Abcdefg1, One special character",
        "'Abcdef1 ', One special character"
    })
    void rejectsEachMissingPasswordRequirementWithoutSaving(String password, String message) throws Exception {
        var before = counts();
        register("Validation", EMAIL, password).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.massage").value("Please check the highlighted fields."))
                .andExpect(jsonPath("$.fieldErrors.password").value(org.hamcrest.Matchers.hasItem(message)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(password))));
        assertEquals(before, counts());
    }

    @Test void rejectsInvalidEmailAndBlankNameWithoutSaving() throws Exception {
        var before = counts();
        register(" ", "not-an-email", "Abcdef1!").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").isArray())
                .andExpect(jsonPath("$.fieldErrors.firstName").isArray());
        assertEquals(before, counts());
    }

    @Test void validRegistrationHashesPasswordAndDuplicateLeavesNoPartialData() throws Exception {
        register("Validation", EMAIL, "Abcdef1!").andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
        String hash = jdbc.queryForObject("select p.hashed_password from password_credential p join customer c on c.password_credential_id = p.id where c.email = ?", String.class, EMAIL);
        assertNotEquals("Abcdef1!", hash);
        assertTrue(encoder.matches("Abcdef1!", hash));
        assertEquals(1, jdbc.queryForObject("select count(*) from account a join customer c on c.id = a.customer_id where c.email = ? and a.name = 'Main Account'", Integer.class, EMAIL));
        var before = counts();
        register("Validation", EMAIL, "Abcdef1!").andExpect(status().isConflict())
                .andExpect(jsonPath("$.massage").value("An account with this email already exists."))
                .andExpect(jsonPath("$.fieldErrors.email[0]").value("An account with this email already exists."));
        assertEquals(before, counts());
    }
}
