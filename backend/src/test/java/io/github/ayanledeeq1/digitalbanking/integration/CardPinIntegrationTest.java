package io.github.ayanledeeq1.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import io.github.ayanledeeq1.digitalbanking.config.CardPinMigration;
import io.github.ayanledeeq1.digitalbanking.dto.customerdto.RegisterCustomerDto;
import io.github.ayanledeeq1.digitalbanking.dto.cardDto.*;
import io.github.ayanledeeq1.digitalbanking.repository.CardRepository;
import io.github.ayanledeeq1.digitalbanking.service.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CardPinIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerService customers;
    @Autowired CardRepository cards;
    @Autowired CardPinCipher cipher;
    @Autowired CardPinMigration migration;
    @Autowired JdbcTemplate jdbc;

    @Test void revealsOnlyOwnersGeneratedPinAfterPasswordVerification() throws Exception {
        customers.saveCustomer(new RegisterCustomerDto("PIN", "Owner", "pin-owner@example.com", "ValidPass1!"));
        customers.saveCustomer(new RegisterCustomerDto("Other", "Owner", "pin-other@example.com", "OtherPass1!"));
        String stored = cards.findFirstByAccountCustomerEmailOrderByAccountIdAsc("pin-owner@example.com").orElseThrow().getEncryptedPin();
        var extra = customers.createAnotherAccount(new io.github.ayanledeeq1.digitalbanking.dto.accountDto.AccountCreateDto(
                "Extra", io.github.ayanledeeq1.digitalbanking.enums.AccountType.SAVINGS), "pin-owner@example.com");
        assertTrue(cards.findByAccountId(extra.getId()).isEmpty());
        assertEquals(stored, cards.findFirstByAccountCustomerEmailOrderByAccountIdAsc("pin-owner@example.com").orElseThrow().getEncryptedPin());
        mvc.perform(post("/api/customers/card/pin").with(user("pin-owner@example.com")).with(csrf())
                .param("customerId", "999").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"ValidPass1!\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.pin").value(cipher.decrypt(stored)))
                .andExpect(jsonPath("$.encryptedPin").doesNotExist());
        for (String password : new String[] {"wrong", "OtherPass1!"}) {
            mvc.perform(post("/api/customers/card/pin").with(user("pin-owner@example.com")).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"" + password + "\"}"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.pin").doesNotExist());
        }
        mvc.perform(post("/api/customers/card/pin").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"ValidPass1!\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/customers/card/pin").with(user("pin-owner@example.com"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"ValidPass1!\"}")).andExpect(status().isForbidden());
    }
    @Test void migrationEncryptsExistingPinsOnceWithoutChangingTheirValues() {
        customers.saveCustomer(new RegisterCustomerDto("Legacy", "Card", "legacy-pin@example.com", "ValidPass1!"));
        Long id = cards.findFirstByAccountCustomerEmailOrderByAccountIdAsc("legacy-pin@example.com").orElseThrow().getId();
        jdbc.update("update card set pin = ? where id = ?", "0007", id);
        migration.run(null);
        String first = jdbc.queryForObject("select pin from card where id = ?", String.class, id);
        assertNotEquals("0007", first); assertEquals("0007", cipher.decrypt(first));
        migration.run(null);
        assertEquals(first, jdbc.queryForObject("select pin from card where id = ?", String.class, id));
    }
    @Test void sensitiveDtosDoNotPrintCredentials() {
        assertFalse(new AtmPinRequestDto("0123").toString().contains("0123"));
        assertFalse(new CardPinResponseDto("0123").toString().contains("0123"));
        assertFalse(new CardPinRevealRequestDto("secret").toString().contains("secret"));
    }
}
