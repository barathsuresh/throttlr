package com.desertrider.throttlr.security.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bitcoinj.crypto.MnemonicCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Passphrase management for account authentication.
 * Generates BIP39 mnemonics (12-word phrases), creates lookup hash and bcrypt
 * hash.
 * Lookup enables fast O(1) account lookup; bcrypt ensures secure storage.
 */
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

    /** Creates HMAC-SHA256 lookup hash for fast account verification. */
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

    /** Encodes passphrase using bcrypt for secure storage. */
    public String hash(String passphrase) {
        return passwordEncoder.encode(normalizeForPasswordEncoder(passphrase));
    }

    /** Verifies raw passphrase against stored bcrypt hash. */
    public boolean matches(String rawPassphrase, String storedHash) {
        String normalizedPassphrase = normalizeForPasswordEncoder(rawPassphrase);
        return passwordEncoder.matches(normalizedPassphrase, storedHash);
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String normalizeForPasswordEncoder(String passphrase) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(passphrase.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to normalize passphrase for hashing", e);
        }
    }
}
