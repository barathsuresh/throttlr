package com.desertrider.throttlr.security.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bitcoinj.crypto.MnemonicCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PassphraseService {
    private final PasswordEncoder passwordEncoder;
    private final String lookupSecret;

    public PassphraseService(
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.lookup-secret}") String lookupSecret) {
        this.passwordEncoder = passwordEncoder;
        this.lookupSecret = lookupSecret;
    }

    public String generatePassphrase() {
        try {
            byte[] entropy = new byte[16];
            new SecureRandom().nextBytes(entropy);

            List<String> words = MnemonicCode.INSTANCE.toMnemonic(entropy);
            log.info("Generated passphrase");
            return String.join(" ", words);
        } catch (Exception e) {
            log.error("Error generating passphrase: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate passphrase", e);
        }
    }

    public String createLookup(String passphrase) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                    lookupSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(key);

            byte[] digest = mac.doFinal(passphrase.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create passphrase lookup", e);
        }
    }

    public String hash(String passphrase) {
        return passwordEncoder.encode(passphrase);
    }

    public boolean matches(String rawPassphrase, String storedHash) {
        return passwordEncoder.matches(rawPassphrase, storedHash);
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
