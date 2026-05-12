package com.desertrider.throttlr.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.desertrider.throttlr.model.Rule;

@Component
public class PatternMatcher {

    private static final char[] FORBIDDEN = {
        '+', '?', '[', ']', '(', ')', '{', '}', '^', '$', '|', '\\'
    };

    private final ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public static boolean isPattern(String clientId) {
        return clientId != null && clientId.contains("*");
    }

    public static void validatePattern(String clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Pattern clientId must not be null");
        }
        for (char c : FORBIDDEN) {
            if (clientId.indexOf(c) >= 0) {
                throw new IllegalArgumentException(
                        "Pattern clientId may only use '*' as a wildcard. Invalid character: " + c);
            }
        }
    }

    public Optional<Rule> findBestMatch(List<Rule> patterns, String clientId) {
        return patterns.stream()
                .filter(rule -> matches(rule.getClientId(), clientId))
                .max(Comparator.comparingInt(rule -> literalLength(rule.getClientId())));
    }

    private boolean matches(String pattern, String clientId) {
        Pattern compiled = patternCache.computeIfAbsent(pattern,
                p -> Pattern.compile(buildRegex(p)));
        return compiled.matcher(clientId).matches();
    }

    private static String buildRegex(String pattern) {
        StringBuilder sb = new StringBuilder("^");
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                sb.append(".*");
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        sb.append("$");
        return sb.toString();
    }

    private static int literalLength(String pattern) {
        int count = 0;
        for (char c : pattern.toCharArray()) {
            if (c != '*') count++;
        }
        return count;
    }
}
