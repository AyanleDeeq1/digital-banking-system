package io.github.ayanledeeq1.digitalbanking.config;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CardConfig {
    @Bean
    public SecureRandom cardRandom() { return new SecureRandom(); }
    @Bean
    public Clock cardClock() { return Clock.system(ZoneId.of("Europe/Stockholm")); }
}
