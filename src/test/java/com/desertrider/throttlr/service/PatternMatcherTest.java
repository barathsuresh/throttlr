package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.model.enums.Algorithm;

class PatternMatcherTest {

    private final PatternMatcher matcher = new PatternMatcher();

    @Test
    void isPatternReturnsTrueWhenClientIdContainsAsterisk() {
        assertTrue(PatternMatcher.isPattern("user:*"));
        assertTrue(PatternMatcher.isPattern("*"));
        assertTrue(PatternMatcher.isPattern("user:*:free:*"));
        assertTrue(PatternMatcher.isPattern("ip:10.0.*"));
    }

    @Test
    void isPatternReturnsFalseForExactClientId() {
        assertFalse(PatternMatcher.isPattern("user:123"));
        assertFalse(PatternMatcher.isPattern("ip:10.0.0.1"));
        assertFalse(PatternMatcher.isPattern("team:free"));
    }

    @Test
    void findBestMatchReturnsEmptyWhenNoPatternsMatch() {
        Rule rule = rule("user:*");
        Optional<Rule> result = matcher.findBestMatch(List.of(rule), "ip:1.2.3.4");
        assertTrue(result.isEmpty());
    }

    @Test
    void findBestMatchReturnsSingleMatchingPattern() {
        Rule rule = rule("user:*");
        Optional<Rule> result = matcher.findBestMatch(List.of(rule), "user:abc");
        assertEquals(rule, result.orElseThrow());
    }

    @Test
    void catchAllMatchesAnything() {
        Rule catchAll = rule("*");
        assertTrue(matcher.findBestMatch(List.of(catchAll), "user:abc:free:basic").isPresent());
        assertTrue(matcher.findBestMatch(List.of(catchAll), "ip:1.2.3.4").isPresent());
        assertTrue(matcher.findBestMatch(List.of(catchAll), "anything").isPresent());
    }

    @Test
    void moreSpecificPatternWinsOverLessSpecific() {
        Rule specific = rule("user:abc:free:*");
        Rule general = rule("user:*");
        Optional<Rule> result = matcher.findBestMatch(List.of(general, specific), "user:abc:free:basic");
        assertEquals(specific, result.orElseThrow());
    }

    @Test
    void equalSpecificityPatternsUseDeterministicTieBreaker() {
        Rule first = rule("user:a*");
        Rule second = rule("user:*b");

        Optional<Rule> result = matcher.findBestMatch(List.of(first, second), "user:ab");

        assertEquals(first, result.orElseThrow());
    }

    @Test
    void catchAllLosesToAnyMoreSpecificPattern() {
        Rule catchAll = rule("*");
        Rule prefixed = rule("user:*");
        Optional<Rule> result = matcher.findBestMatch(List.of(catchAll, prefixed), "user:abc");
        assertEquals(prefixed, result.orElseThrow());
    }

    @Test
    void patternWithMultipleWildcardsMatchesCorrectly() {
        Rule rule = rule("user:*:free:*");
        assertTrue(matcher.findBestMatch(List.of(rule), "user:abc:free:basic").isPresent());
        assertTrue(matcher.findBestMatch(List.of(rule), "user:xyz:free:premium").isPresent());
        assertTrue(matcher.findBestMatch(List.of(rule), "user:abc:paid:basic").isEmpty());
    }

    @Test
    void validatePatternAcceptsValidWildcardPatterns() {
        assertDoesNotThrow(() -> PatternMatcher.validatePattern("user:*"));
        assertDoesNotThrow(() -> PatternMatcher.validatePattern("*"));
        assertDoesNotThrow(() -> PatternMatcher.validatePattern("user:*:free:*"));
        assertDoesNotThrow(() -> PatternMatcher.validatePattern("ip:10.0.*"));
    }

    @Test
    void validatePatternRejectsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> PatternMatcher.validatePattern(null));
    }

    @Test
    void validatePatternRejectsRegexSpecialChars() {
        assertThrows(IllegalArgumentException.class,
                () -> PatternMatcher.validatePattern("user:[0-9]*"));
        assertThrows(IllegalArgumentException.class,
                () -> PatternMatcher.validatePattern("user:.+"));
        assertThrows(IllegalArgumentException.class,
                () -> PatternMatcher.validatePattern("user:(abc)*"));
        assertThrows(IllegalArgumentException.class,
                () -> PatternMatcher.validatePattern("user:{abc}*"));
    }

    @Test
    void validatePatternRejectsConsecutiveWildcards() {
        assertThrows(IllegalArgumentException.class,
                () -> PatternMatcher.validatePattern("user:**"));
        assertThrows(IllegalArgumentException.class,
                () -> PatternMatcher.validatePattern("**"));
        assertThrows(IllegalArgumentException.class,
                () -> PatternMatcher.validatePattern("user:**:free"));
    }

    private Rule rule(String clientId) {
        return Rule.builder()
                .clientId(clientId)
                .algorithm(Algorithm.FIXED_WINDOW)
                .limitPerWindow(10)
                .windowMs(60_000)
                .build();
    }
}
