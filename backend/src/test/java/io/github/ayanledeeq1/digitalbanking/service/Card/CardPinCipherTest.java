package io.github.ayanledeeq1.digitalbanking.service.Card;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import io.github.ayanledeeq1.digitalbanking.service.CardPinCipher;

class CardPinCipherTest {
    private final CardPinCipher cipher = new CardPinCipher(Base64.getEncoder().encodeToString(new byte[32]));

    @Test void encryptsWithFreshNoncesAndPreservesLeadingZeros() {
        String first = cipher.encrypt("0123"); String second = cipher.encrypt("0123");
        assertNotEquals("0123", first); assertNotEquals(first, second);
        assertTrue(CardPinCipher.isEncrypted(first)); assertEquals("0123", cipher.decrypt(first));
        assertTrue(cipher.matches("0123", first)); assertFalse(cipher.matches("9999", first));
        assertFalse(cipher.matches(null, first));
    }
    @Test void rejectsTamperingAndWrongKeys() {
        String original = cipher.encrypt("0123");
        byte[] envelope = Base64.getDecoder().decode(original.substring(3)); envelope[20] ^= 1;
        assertThrows(IllegalStateException.class, () -> cipher.decrypt("v1:" + Base64.getEncoder().encodeToString(envelope)));
        byte[] key = new byte[32]; key[0] = 1;
        CardPinCipher other = new CardPinCipher(Base64.getEncoder().encodeToString(key));
        assertThrows(IllegalStateException.class, () -> other.decrypt(original));
    }
    @Test void rejectsMalformedKeysAndPins() {
        assertThrows(IllegalStateException.class, () -> new CardPinCipher("invalid"));
        assertThrows(IllegalStateException.class, () -> new CardPinCipher(Base64.getEncoder().encodeToString(new byte[16])));
        for (String pin : new String[] {null, "", "123", "12345", "abcd"}) {
            assertThrows(IllegalArgumentException.class, () -> cipher.encrypt(pin));
        }
        assertThrows(IllegalStateException.class, () -> cipher.decrypt("0123"));
    }
}
