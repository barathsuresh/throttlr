package com.desertrider.throttlr.validation;

public final class InputLimits {
    public static final int APP_NAME_MAX_LENGTH = 100;
    public static final int CLIENT_ID_MAX_LENGTH = 200;
    public static final int PASSPHRASE_MAX_LENGTH = 300;
    public static final int APP_KEY_MAX_LENGTH = 500;

    private InputLimits() {
    }

    public static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }
}
