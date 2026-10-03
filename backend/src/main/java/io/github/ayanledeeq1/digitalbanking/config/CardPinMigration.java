package io.github.ayanledeeq1.digitalbanking.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.github.ayanledeeq1.digitalbanking.service.CardPinCipher;

// Transitional V1 conversion of existing plaintext PINs before the application is ready.
@Component
public class CardPinMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final CardPinCipher cipher;
    public CardPinMigration(JdbcTemplate jdbc, CardPinCipher cipher) { this.jdbc = jdbc; this.cipher = cipher; }

    @Override @Transactional
    public void run(ApplicationArguments arguments) {
        jdbc.query("select id, pin from card", (org.springframework.jdbc.core.RowCallbackHandler) row -> {
            String stored = row.getString("pin");
            if (stored != null && stored.matches("[0-9]{4}")) {
                jdbc.update("update card set pin = ? where id = ? and pin = ?", cipher.encrypt(stored), row.getLong("id"), stored);
            } else {
                // Refuse to start with a changed/missing key or malformed existing ciphertext.
                cipher.decrypt(stored);
            }
        });
    }
}
