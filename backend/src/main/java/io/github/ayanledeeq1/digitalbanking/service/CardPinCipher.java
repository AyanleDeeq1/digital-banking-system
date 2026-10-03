package io.github.ayanledeeq1.digitalbanking.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CardPinCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public CardPinCipher(@Value("${app.card-pin.encryption-key}") String encodedKey) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encodedKey); }
        catch (IllegalArgumentException failure) { throw new IllegalStateException("Card PIN encryption key must be Base64"); }
        if (bytes.length != 32) throw new IllegalStateException("Card PIN encryption key must contain 32 bytes");
        key = new SecretKeySpec(bytes, "AES");
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.matches("v1:[A-Za-z0-9+/]{43}=");
    }

    public String encrypt(String pin) {
        if (pin == null || !pin.matches("[0-9]{4}")) throw new IllegalArgumentException("PIN must contain exactly 4 digits");
        byte[] nonce = new byte[12]; random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD("URBank card PIN v1".getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(pin.getBytes(StandardCharsets.UTF_8));
            byte[] envelope = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, envelope, 0, nonce.length);
            System.arraycopy(encrypted, 0, envelope, nonce.length, encrypted.length);
            return "v1:" + Base64.getEncoder().encodeToString(envelope);
        } catch (GeneralSecurityException failure) { throw new IllegalStateException("Unable to encrypt card PIN"); }
    }

    public String decrypt(String value) {
        if (!isEncrypted(value)) throw new IllegalStateException("Invalid encrypted card PIN");
        try {
            byte[] envelope = Base64.getDecoder().decode(value.substring(3));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, envelope, 0, 12));
            cipher.updateAAD("URBank card PIN v1".getBytes(StandardCharsets.UTF_8));
            String pin = new String(cipher.doFinal(envelope, 12, envelope.length - 12), StandardCharsets.UTF_8);
            if (!pin.matches("[0-9]{4}")) throw new IllegalStateException("Invalid encrypted card PIN");
            return pin;
        } catch (GeneralSecurityException | IllegalArgumentException failure) {
            throw new IllegalStateException("Unable to decrypt card PIN with the configured key");
        }
    }

    public boolean matches(String pin, String encrypted) {
        return pin != null && pin.matches("[0-9]{4}") && MessageDigest.isEqual(
                pin.getBytes(StandardCharsets.UTF_8), decrypt(encrypted).getBytes(StandardCharsets.UTF_8));
    }
}
