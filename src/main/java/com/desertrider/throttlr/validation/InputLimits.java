package com.desertrider.throttlr.validation;

/** Constants and methods for input validation across the application. */
public final class InputLimits {
    /** App display name length limit. */
    public static final int APP_NAME_MAX_LENGTH = 100;

    /** Client identifier (exact or pattern) length limit. */
    public static final int CLIENT_ID_MAX_LENGTH = 200;

    /** Passphrase length limit (BIP39 mnemonic buffer). */
    public static final int PASSPHRASE_MAX_LENGTH = 300;

    /** App key length limit (base64 encoded + prefix). */
    public static final int APP_KEY_MAX_LENGTH = 500;

    private InputLimits() {
    }

    /**
     * Validates string against max length. Throws IllegalArgumentException if
     * exceeded.
     */
    public static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }
}
