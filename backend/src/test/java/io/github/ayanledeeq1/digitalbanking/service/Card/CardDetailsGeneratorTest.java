package io.github.ayanledeeq1.digitalbanking.service.Card;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.security.SecureRandom;
import java.time.*;
import org.junit.jupiter.api.Test;
import io.github.ayanledeeq1.digitalbanking.service.CardDetailsGenerator;

class CardDetailsGeneratorTest {
    @Test void pinHasFourRandomDigitsAndPreservesLeadingZeros() {
        SecureRandom random = mock(SecureRandom.class);
        when(random.nextInt(10)).thenReturn(0, 1, 2, 3);
        CardDetailsGenerator generator = new CardDetailsGenerator(random, Clock.systemUTC());
        assertEquals("0123", generator.generatePin());
        verify(random, times(4)).nextInt(10);
    }
    @Test void preservesLeadingZerosAndGeneratesIndependentDigits() {
        SecureRandom random = mock(SecureRandom.class);
        when(random.nextInt(10)).thenReturn(0, 0, 7, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 0, 4, 2);
        CardDetailsGenerator generator = new CardDetailsGenerator(random, Clock.systemUTC());
        assertEquals("0071234567890123", generator.generateCardNumber());
        assertEquals("042", generator.generateCvc2());
        verify(random, times(19)).nextInt(10);
    }
    @Test void expiryUsesStockholmDateAndThreeCalendarYears() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-01T22:30:00Z"), ZoneId.of("Europe/Stockholm"));
        assertEquals(LocalDate.of(2029, 10, 2), new CardDetailsGenerator(mock(SecureRandom.class), clock).expiryDate());
    }
    @Test void leapDayExpiresOnFebruary28() {
        Clock clock = Clock.fixed(Instant.parse("2024-02-29T12:00:00Z"), ZoneId.of("Europe/Stockholm"));
        assertEquals(LocalDate.of(2027, 2, 28), new CardDetailsGenerator(mock(SecureRandom.class), clock).expiryDate());
    }
}
