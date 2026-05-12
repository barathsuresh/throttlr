# Wildcard & Pattern Rule Matching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow rules with glob-style `clientId` patterns (e.g. `user:*`, `user:*:free:*`, `*`) so any dynamic identifier is rate-limited without pre-registration.

**Architecture:** On exact clientId miss in `RateLimitService`, load all pattern rules for the app (Redis-cached list), find the most-specific matching pattern via glob-to-regex matching, synthesize a rule with the actual clientId (so each caller gets an independent Redis counter), and run the limiter. A new `PatternMatcher` component owns matching/scoring logic in isolation.

**Tech Stack:** Java 25, Spring Boot 4, Redis (pattern list cache key `rule-patterns:<appId>`), MongoDB (`@Query` regex), Lombok, JUnit 5.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `src/main/java/com/desertrider/throttlr/service/PatternMatcher.java` | Create | Glob matching, specificity scoring, pattern validation |
| `src/test/java/com/desertrider/throttlr/service/PatternMatcherTest.java` | Create | Unit tests for PatternMatcher |
| `src/main/java/com/desertrider/throttlr/repository/RuleRepository.java` | Modify | Add `findPatternsByAppId` query |
| `src/main/java/com/desertrider/throttlr/service/cache/RuleCacheService.java` | Modify | Add `findPatternsByAppId` and `deletePatternCache` |
| `src/main/java/com/desertrider/throttlr/service/cache/RedisRuleCacheService.java` | Modify | Implement pattern list cache at `rule-patterns:<appId>` |
| `src/main/java/com/desertrider/throttlr/service/RuleService.java` | Modify | Validate pattern clientIds; invalidate pattern cache on writes |
| `src/test/java/com/desertrider/throttlr/service/RuleServiceTest.java` | Modify | Add pattern validation tests |
| `src/main/java/com/desertrider/throttlr/service/RateLimitService.java` | Modify | Pattern fallback after exact miss; synthesizeRule helper |
| `src/test/java/com/desertrider/throttlr/service/RateLimitServiceTest.java` | Modify | Add pattern match tests; update existing constructor calls |

---

## Task 1: PatternMatcher — glob matching and specificity

**Files:**
- Create: `src/main/java/com/desertrider/throttlr/service/PatternMatcher.java`
- Create: `src/test/java/com/desertrider/throttlr/service/PatternMatcherTest.java`

- [ ] **Step 1: Write the failing tests**

```java
// src/test/java/com/desertrider/throttlr/service/PatternMatcherTest.java
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
        // user:abc:free:* has more literal chars (11) than user:* (5)
        Rule specific = rule("user:abc:free:*");
        Rule general = rule("user:*");
        Optional<Rule> result = matcher.findBestMatch(List.of(general, specific), "user:abc:free:basic");
        assertEquals(specific, result.orElseThrow());
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

    private Rule rule(String clientId) {
        return Rule.builder()
                .clientId(clientId)
                .algorithm(Algorithm.FIXED_WINDOW)
                .limitPerWindow(10)
                .windowMs(60_000)
                .build();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /path/to/repo
./mvnw test -pl . -Dtest=PatternMatcherTest -q 2>&1 | tail -5
```
Expected: compilation error — `PatternMatcher` not found.

- [ ] **Step 3: Create PatternMatcher**

```java
// src/main/java/com/desertrider/throttlr/service/PatternMatcher.java
package com.desertrider.throttlr.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.desertrider.throttlr.model.Rule;

@Component
public class PatternMatcher {

    private static final char[] FORBIDDEN = {
        '+', '?', '[', ']', '(', ')', '{', '}', '.', '^', '$', '|', '\\'
    };

    public static boolean isPattern(String clientId) {
        return clientId != null && clientId.contains("*");
    }

    public static void validatePattern(String clientId) {
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
        return Pattern.matches(buildRegex(pattern), clientId);
    }

    private String buildRegex(String pattern) {
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

    private int literalLength(String pattern) {
        int count = 0;
        for (char c : pattern.toCharArray()) {
            if (c != '*') count++;
        }
        return count;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw test -pl . -Dtest=PatternMatcherTest -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`, 12 tests passing.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/desertrider/throttlr/service/PatternMatcher.java \
        src/test/java/com/desertrider/throttlr/service/PatternMatcherTest.java
git commit -m "feat: add PatternMatcher for glob-style clientId matching"
```

---

## Task 2: Repository — findPatternsByAppId

**Files:**
- Modify: `src/main/java/com/desertrider/throttlr/repository/RuleRepository.java`

- [ ] **Step 1: Add the query method**

Open `src/main/java/com/desertrider/throttlr/repository/RuleRepository.java`. Add import and method:

```java
package com.desertrider.throttlr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.desertrider.throttlr.model.Rule;

public interface RuleRepository extends MongoRepository<Rule, String> {
    List<Rule> findByAppId(String appId);

    Optional<Rule> findByAppIdAndClientId(String appId, String clientId);

    Page<Rule> findByAppId(String appId, Pageable pageable);

    void deleteByAppIdAndClientId(String appId, String clientId);

    void deleteByAppId(String appId);

    long countByAppId(String appId);

    @Query("{ 'appId': ?0, 'clientId': { $regex: '\\\\*' } }")
    List<Rule> findPatternsByAppId(String appId);
}
```

The `@Query` annotation: Java string `"\\\\*"` → annotation value `\\*` → MongoDB regex `\*` → matches any clientId containing the literal `*` character.

- [ ] **Step 2: Verify compile**

```bash
./mvnw compile -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/desertrider/throttlr/repository/RuleRepository.java
git commit -m "feat: add findPatternsByAppId query to RuleRepository"
```

---

## Task 3: Cache — pattern list cache in Redis

**Files:**
- Modify: `src/main/java/com/desertrider/throttlr/service/cache/RuleCacheService.java`
- Modify: `src/main/java/com/desertrider/throttlr/service/cache/RedisRuleCacheService.java`

- [ ] **Step 1: Add methods to the interface**

Replace the contents of `src/main/java/com/desertrider/throttlr/service/cache/RuleCacheService.java`:

```java
package com.desertrider.throttlr.service.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.desertrider.throttlr.model.Rule;

public interface RuleCacheService {
    Optional<Rule> findByAppIdAndClientId(String appId, String clientId);

    List<Rule> findPatternsByAppId(String appId);

    void put(Rule rule);

    default void put(Rule rule, Duration ttl) {
        put(rule);
    }

    void delete(String appId, String clientId);

    void deletePatternCache(String appId);
}
```

- [ ] **Step 2: Verify compile fails (expected — RedisRuleCacheService doesn't implement new methods yet)**

```bash
./mvnw compile -q 2>&1 | grep "error" | head -5
```
Expected: errors about `RedisRuleCacheService` not implementing `findPatternsByAppId` and `deletePatternCache`.

- [ ] **Step 3: Implement pattern cache in RedisRuleCacheService**

Replace the contents of `src/main/java/com/desertrider/throttlr/service/cache/RedisRuleCacheService.java`:

```java
package com.desertrider.throttlr.service.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.repository.RuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisRuleCacheService implements RuleCacheService {
    private static final String RULE_KEY_PREFIX = "rule:";
    private static final String PATTERN_KEY_PREFIX = "rule-patterns:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RuleRepository ruleRepository;
    private final Duration ttl;

    public RedisRuleCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            RuleRepository ruleRepository,
            @Value("${app.cache.rule-ttl-ms:300000}") long ttlMs) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ruleRepository = ruleRepository;
        this.ttl = Duration.ofMillis(ttlMs);
    }

    @Override
    public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
        return readFromRedis(appId, clientId)
                .or(() -> ruleRepository.findByAppIdAndClientId(appId, clientId).map(rule -> {
                    put(rule);
                    return rule;
                }));
    }

    @Override
    public List<Rule> findPatternsByAppId(String appId) {
        try {
            String value = redisTemplate.opsForValue().get(patternKey(appId));
            if (value != null) {
                return objectMapper.readValue(value, new TypeReference<List<Rule>>() {});
            }
        } catch (Exception ignored) {
            // fall through to MongoDB
        }
        List<Rule> patterns = ruleRepository.findPatternsByAppId(appId);
        cachePatterns(appId, patterns);
        return patterns;
    }

    @Override
    public void put(Rule rule) {
        put(rule, ttl);
    }

    @Override
    public void put(Rule rule, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(
                    ruleKey(rule.getAppId(), rule.getClientId()),
                    objectMapper.writeValueAsString(rule),
                    ttl);
        } catch (Exception ignored) {
            // Mongo remains the source of truth if Redis is unavailable.
        }
    }

    @Override
    public void delete(String appId, String clientId) {
        try {
            redisTemplate.delete(ruleKey(appId, clientId));
        } catch (Exception ignored) {
            // The Mongo source of truth still wins on the next cache miss.
        }
    }

    @Override
    public void deletePatternCache(String appId) {
        try {
            redisTemplate.delete(patternKey(appId));
        } catch (Exception ignored) {
            // The Mongo source of truth still wins on the next cache miss.
        }
    }

    private Optional<Rule> readFromRedis(String appId, String clientId) {
        try {
            String value = redisTemplate.opsForValue().get(ruleKey(appId, clientId));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, Rule.class));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private void cachePatterns(String appId, List<Rule> patterns) {
        try {
            redisTemplate.opsForValue().set(
                    patternKey(appId),
                    objectMapper.writeValueAsString(patterns),
                    ttl);
        } catch (Exception ignored) {
            // Mongo remains the source of truth if Redis is unavailable.
        }
    }

    private String ruleKey(String appId, String clientId) {
        return RULE_KEY_PREFIX + appId + ":" + clientId;
    }

    private String patternKey(String appId) {
        return PATTERN_KEY_PREFIX + appId;
    }
}
```

Note: the old code used `"rule:%s:%s".formatted(appId, clientId)` — the new `ruleKey` method produces the same string. Verify if needed.

- [ ] **Step 4: Verify compile succeeds**

```bash
./mvnw compile -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/desertrider/throttlr/service/cache/RuleCacheService.java \
        src/main/java/com/desertrider/throttlr/service/cache/RedisRuleCacheService.java
git commit -m "feat: add pattern list cache to RuleCacheService"
```

---

## Task 4: RuleService — validate patterns, invalidate cache

**Files:**
- Modify: `src/main/java/com/desertrider/throttlr/service/RuleService.java`
- Modify: `src/test/java/com/desertrider/throttlr/service/RuleServiceTest.java`

- [ ] **Step 1: Write failing tests**

Add these two tests to `RuleServiceTest` (inside the class, before the inner recording classes):

```java
@Test
void createRuleRejectsPatternWithRegexSpecialChars() {
    RuleService ruleService = new RuleService(null, null, null);

    assertThrows(
            IllegalArgumentException.class,
            () -> ruleService.createRule(
                    "account-1",
                    "app-1",
                    new CreateRuleRequest("user:[0-9]*", Algorithm.FIXED_WINDOW, 10, 60_000)));
}

@Test
void createRuleInvalidatesPatternCacheForPatternClientId() {
    String accountId = "account-1";
    String appId = "app-1";
    String clientId = "user:*";

    App app = App.builder()
            .id(appId)
            .accountId(accountId)
            .ruleCount(0)
            .build();

    Rule rule = Rule.builder()
            .id("rule-1")
            .appId(appId)
            .accountId(accountId)
            .clientId(clientId)
            .algorithm(Algorithm.FIXED_WINDOW)
            .limitPerWindow(10)
            .windowMs(60_000)
            .createdAt(1L)
            .updatedAt(1L)
            .build();

    RecordingAppRepository appRepository = new RecordingAppRepository(app);
    RecordingRuleRepository ruleRepository = new RecordingRuleRepository(rule);
    TrackingRuleCacheService ruleCacheService = new TrackingRuleCacheService();

    RuleService ruleService = new RuleService(ruleRepository.proxy(), appRepository.proxy(), ruleCacheService);
    ruleService.createRule(accountId, appId,
            new CreateRuleRequest(clientId, Algorithm.FIXED_WINDOW, 10, 60_000));

    assertEquals(appId, ruleCacheService.deletedPatternCacheAppId);
}
```

Also add `TrackingRuleCacheService` inner class (replaces `RecordingRuleCacheService` — add alongside it, don't remove existing one since other tests use it):

```java
private static final class TrackingRuleCacheService implements RuleCacheService {
    private Rule cachedRule;
    private String deletedAppId;
    private String deletedClientId;
    private String deletedPatternCacheAppId;

    @Override
    public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
        return Optional.empty();
    }

    @Override
    public List<Rule> findPatternsByAppId(String appId) {
        return List.of();
    }

    @Override
    public void put(Rule rule) {
        cachedRule = rule;
    }

    @Override
    public void delete(String appId, String clientId) {
        deletedAppId = appId;
        deletedClientId = clientId;
    }

    @Override
    public void deletePatternCache(String appId) {
        deletedPatternCacheAppId = appId;
    }
}
```

Also update existing `RecordingRuleCacheService` to implement the two new interface methods:

```java
private static final class RecordingRuleCacheService implements RuleCacheService {
    private Rule cachedRule;
    private String deletedAppId;
    private String deletedClientId;

    @Override
    public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
        return Optional.empty();
    }

    @Override
    public List<Rule> findPatternsByAppId(String appId) {
        return List.of();
    }

    @Override
    public void put(Rule rule) {
        cachedRule = rule;
    }

    @Override
    public void delete(String appId, String clientId) {
        deletedAppId = appId;
        deletedClientId = clientId;
    }

    @Override
    public void deletePatternCache(String appId) {
    }
}
```

Add the import `import java.util.List;` at the top of `RuleServiceTest.java`.

- [ ] **Step 2: Run tests to verify they fail**

```bash
./mvnw test -pl . -Dtest=RuleServiceTest -q 2>&1 | tail -10
```
Expected: `createRuleRejectsPatternWithRegexSpecialChars` FAIL (no validation yet), `createRuleInvalidatesPatternCacheForPatternClientId` FAIL (no cache invalidation yet).

- [ ] **Step 3: Update RuleService**

In `src/main/java/com/desertrider/throttlr/service/RuleService.java`:

Add import at top:
```java
import com.desertrider.throttlr.service.PatternMatcher;
```

In `validateCreateRuleRequest`, add after the `InputLimits.requireMaxLength` call:
```java
if (PatternMatcher.isPattern(request.clientId())) {
    PatternMatcher.validatePattern(request.clientId());
}
```

In `createRule`, after `ruleCacheService.put(savedRule);`:
```java
if (PatternMatcher.isPattern(savedRule.getClientId())) {
    ruleCacheService.deletePatternCache(appId);
}
```

In `updateRule`, after `ruleCacheService.put(savedRule);`:
```java
if (PatternMatcher.isPattern(savedRule.getClientId())) {
    ruleCacheService.deletePatternCache(appId);
}
```

In `deleteRule`, after `ruleCacheService.delete(appId, clientId);`:
```java
if (PatternMatcher.isPattern(clientId)) {
    ruleCacheService.deletePatternCache(appId);
}
```

The full updated `validateCreateRuleRequest` method:
```java
private void validateCreateRuleRequest(CreateRuleRequest request) {
    if (request == null) {
        throw new IllegalArgumentException("Rule request is required");
    }

    if (!StringUtils.hasText(request.clientId())) {
        throw new IllegalArgumentException("Client id is required");
    }
    InputLimits.requireMaxLength(
            request.clientId(),
            InputLimits.CLIENT_ID_MAX_LENGTH,
            "Client id must be at most 200 characters");

    if (PatternMatcher.isPattern(request.clientId())) {
        PatternMatcher.validatePattern(request.clientId());
    }

    if (request.algorithm() == null) {
        throw new IllegalArgumentException("Algorithm is required");
    }

    if (request.limitPerWindow() <= 0) {
        throw new IllegalArgumentException("Limit per window must be greater than 0");
    }

    if (request.windowMs() <= 0) {
        throw new IllegalArgumentException("Window must be greater than 0");
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw test -pl . -Dtest=RuleServiceTest -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/desertrider/throttlr/service/RuleService.java \
        src/test/java/com/desertrider/throttlr/service/RuleServiceTest.java
git commit -m "feat: validate pattern clientIds and invalidate pattern cache in RuleService"
```

---

## Task 5: RateLimitService — pattern fallback

**Files:**
- Modify: `src/main/java/com/desertrider/throttlr/service/RateLimitService.java`
- Modify: `src/test/java/com/desertrider/throttlr/service/RateLimitServiceTest.java`

- [ ] **Step 1: Write failing tests**

In `RateLimitServiceTest.java`, add these tests (inside the class, before inner recording classes):

```java
@Test
void patternRuleAppliedOnExactMiss() {
    String appKey = "throttlr_live_test-key";

    RecordingAppKeyCacheService appKeyCacheService = new RecordingAppKeyCacheService();
    AppKeyService appKeyService = new AppKeyService(
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
            "test-app-key-secret",
            appKeyCacheService);

    App app = App.builder()
            .id("app-1")
            .apiKeyLookup(appKeyService.createLookup(appKey))
            .apiKeyHash(appKeyService.hash(appKey))
            .build();
    appKeyCacheService.app = app;

    Rule patternRule = Rule.builder()
            .id("rule-pat")
            .appId(app.getId())
            .clientId("user:*")
            .algorithm(Algorithm.FIXED_WINDOW)
            .limitPerWindow(10)
            .windowMs(60_000)
            .build();

    PatternRuleCacheService ruleCacheService = new PatternRuleCacheService(patternRule);
    RecordingFixedWindowRateLimiter fixedWindowRateLimiter = new RecordingFixedWindowRateLimiter();
    RecordingTokenBucketRateLimiter tokenBucketRateLimiter = new RecordingTokenBucketRateLimiter();
    RecordingSlidingWindowRateLimiter slidingWindowRateLimiter = new RecordingSlidingWindowRateLimiter();
    RecordingAnalyticsService analyticsService = new RecordingAnalyticsService();
    PatternMatcher patternMatcher = new PatternMatcher();

    RateLimitService rateLimitService = new RateLimitService(
            appKeyService,
            ruleCacheService,
            fixedWindowRateLimiter,
            tokenBucketRateLimiter,
            slidingWindowRateLimiter,
            analyticsService,
            patternMatcher);

    rateLimitService.check(appKey, new CheckRequest("user:abc123"));

    // limiter receives synthesized rule with actual clientId, not "*"
    assertNotNull(fixedWindowRateLimiter.rule);
    assertEquals("user:abc123", fixedWindowRateLimiter.rule.getClientId());
    assertEquals(10, fixedWindowRateLimiter.rule.getLimitPerWindow());
}

@Test
void catchAllPatternMatchesWhenNoExactRule() {
    String appKey = "throttlr_live_test-key2";

    RecordingAppKeyCacheService appKeyCacheService2 = new RecordingAppKeyCacheService();
    AppKeyService appKeyService2 = new AppKeyService(
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
            "test-app-key-secret",
            appKeyCacheService2);

    App app = App.builder()
            .id("app-2")
            .apiKeyLookup(appKeyService2.createLookup(appKey))
            .apiKeyHash(appKeyService2.hash(appKey))
            .build();
    appKeyCacheService2.app = app;

    Rule catchAll = Rule.builder()
            .id("rule-catch")
            .appId(app.getId())
            .clientId("*")
            .algorithm(Algorithm.FIXED_WINDOW)
            .limitPerWindow(5)
            .windowMs(30_000)
            .build();

    PatternRuleCacheService ruleCacheService = new PatternRuleCacheService(catchAll);
    RecordingFixedWindowRateLimiter fixedWindowRateLimiter = new RecordingFixedWindowRateLimiter();
    RecordingTokenBucketRateLimiter tokenBucketRateLimiter = new RecordingTokenBucketRateLimiter();
    RecordingSlidingWindowRateLimiter slidingWindowRateLimiter = new RecordingSlidingWindowRateLimiter();
    RecordingAnalyticsService analyticsService = new RecordingAnalyticsService();
    PatternMatcher patternMatcher = new PatternMatcher();

    RateLimitService rateLimitService = new RateLimitService(
            appKeyService2,
            ruleCacheService,
            fixedWindowRateLimiter,
            tokenBucketRateLimiter,
            slidingWindowRateLimiter,
            analyticsService,
            patternMatcher);

    CheckResponse response = rateLimitService.check(appKey, new CheckRequest("ip:203.0.113.10"));

    assertNotNull(fixedWindowRateLimiter.rule);
    assertEquals("ip:203.0.113.10", fixedWindowRateLimiter.rule.getClientId());
    assertEquals(5, fixedWindowRateLimiter.rule.getLimitPerWindow());
    assertTrue(response.allowed());
}
```

Add `PatternRuleCacheService` inner class (no exact match, returns pattern list):

```java
private static final class PatternRuleCacheService implements RuleCacheService {
    private final Rule patternRule;

    private PatternRuleCacheService(Rule patternRule) {
        this.patternRule = patternRule;
    }

    @Override
    public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
        return Optional.empty(); // no exact match
    }

    @Override
    public List<Rule> findPatternsByAppId(String appId) {
        return List.of(patternRule);
    }

    @Override
    public void put(Rule rule) {}

    @Override
    public void delete(String appId, String clientId) {}

    @Override
    public void deletePatternCache(String appId) {}
}
```

Update `RecordingRuleCacheService` (used in existing tests) to implement the two new interface methods:

```java
private static final class RecordingRuleCacheService implements RuleCacheService {
    private final Rule rule;
    private String appId;
    private String clientId;

    private RecordingRuleCacheService(Rule rule) {
        this.rule = rule;
    }

    @Override
    public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
        this.appId = appId;
        this.clientId = clientId;
        return Optional.of(rule);
    }

    @Override
    public List<Rule> findPatternsByAppId(String appId) {
        return List.of();
    }

    @Override
    public void put(Rule rule) {}

    @Override
    public void delete(String appId, String clientId) {}

    @Override
    public void deletePatternCache(String appId) {}
}
```

Update the existing `checkRejectsOverlongClientId` test constructor call to pass `null` for `patternMatcher`:

```java
@Test
void checkRejectsOverlongClientId() {
    RateLimitService rateLimitService = new RateLimitService(null, null, null, null, null, null, null);

    assertThrows(
            IllegalArgumentException.class,
            () -> rateLimitService.check("app-key", new CheckRequest("c".repeat(201))));
}
```

Update `checkUsesCachedRuleAndRecordsAnalytics` to pass a `PatternMatcher` instance as the 7th arg:

```java
RateLimitService rateLimitService = new RateLimitService(
        appKeyService,
        ruleCacheService,
        fixedWindowRateLimiter,
        tokenBucketRateLimiter,
        slidingWindowRateLimiter,
        analyticsService,
        new PatternMatcher());
```

Add imports at the top of `RateLimitServiceTest.java`:
```java
import java.util.List;
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./mvnw test -pl . -Dtest=RateLimitServiceTest -q 2>&1 | tail -10
```
Expected: compilation error — `RateLimitService` constructor still takes 6 args.

- [ ] **Step 3: Update RateLimitService**

Replace `src/main/java/com/desertrider/throttlr/service/RateLimitService.java`:

```java
package com.desertrider.throttlr.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.desertrider.throttlr.dto.request.CheckRequest;
import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.model.enums.Algorithm;
import com.desertrider.throttlr.service.analytics.AnalyticsService;
import com.desertrider.throttlr.service.cache.RuleCacheService;
import com.desertrider.throttlr.service.limiter.FixedWindowRateLimiter;
import com.desertrider.throttlr.service.limiter.SlidingWindowRateLimiter;
import com.desertrider.throttlr.service.limiter.TokenBucketRateLimiter;
import com.desertrider.throttlr.validation.InputLimits;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final AppKeyService appKeyService;
    private final RuleCacheService ruleCacheService;
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    private final AnalyticsService analyticsService;
    private final PatternMatcher patternMatcher;

    public CheckResponse check(String appKey, CheckRequest request) {
        if (request == null || !StringUtils.hasText(request.clientId())) {
            throw new IllegalArgumentException("Client id is required");
        }
        InputLimits.requireMaxLength(
                request.clientId(),
                InputLimits.CLIENT_ID_MAX_LENGTH,
                "Client id must be at most 200 characters");

        App app = appKeyService.validateAppKey(appKey);
        String clientId = request.clientId().trim();

        CheckResponse response = ruleCacheService.findByAppIdAndClientId(app.getId(), clientId)
                .or(() -> patternMatcher
                        .findBestMatch(ruleCacheService.findPatternsByAppId(app.getId()), clientId)
                        .map(pattern -> synthesizeRule(pattern, clientId)))
                .map(this::checkConfiguredRule)
                .orElseGet(this::allowWithoutRule);

        analyticsService.record(app, clientId, response);
        return response;
    }

    private Rule synthesizeRule(Rule pattern, String actualClientId) {
        return Rule.builder()
                .id(pattern.getId())
                .appId(pattern.getAppId())
                .accountId(pattern.getAccountId())
                .clientId(actualClientId)
                .algorithm(pattern.getAlgorithm())
                .limitPerWindow(pattern.getLimitPerWindow())
                .windowMs(pattern.getWindowMs())
                .createdAt(pattern.getCreatedAt())
                .updatedAt(pattern.getUpdatedAt())
                .build();
    }

    private CheckResponse checkConfiguredRule(Rule rule) {
        if (rule.getAlgorithm() == Algorithm.FIXED_WINDOW) {
            return fixedWindowRateLimiter.check(rule);
        }

        if (rule.getAlgorithm() == Algorithm.TOKEN_BUCKET) {
            return tokenBucketRateLimiter.check(rule);
        }

        if (rule.getAlgorithm() == Algorithm.SLIDING_WINDOW) {
            return slidingWindowRateLimiter.check(rule);
        }

        throw new IllegalArgumentException("Unsupported rate limit algorithm: " + rule.getAlgorithm());
    }

    private CheckResponse allowWithoutRule() {
        return new CheckResponse(true, Integer.MAX_VALUE, 0, 0);
    }
}
```

- [ ] **Step 4: Run all tests**

```bash
./mvnw test -q 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`, all tests pass (existing 47 + new pattern tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/desertrider/throttlr/service/RateLimitService.java \
        src/test/java/com/desertrider/throttlr/service/RateLimitServiceTest.java
git commit -m "feat: add pattern rule fallback to RateLimitService"
```

---

## Self-Review Checklist

**Spec coverage:**
- ✅ Pattern rule created with `clientId` containing `*` — Task 4 (RuleService validation)
- ✅ `*` anywhere in clientId = pattern — PatternMatcher.isPattern
- ✅ Exact match first — Task 5, `.findByAppIdAndClientId` before pattern lookup
- ✅ Best pattern match on exact miss — Task 5, `patternMatcher.findBestMatch`
- ✅ Independent counters via synthesizeRule — Task 5
- ✅ Specificity by literal length — Task 1, `literalLength`
- ✅ Redis pattern list cache — Task 3
- ✅ Pattern cache invalidated on create/update/delete — Task 4
- ✅ Validation: only `*` allowed as wildcard char — Task 1, `validatePattern`
- ✅ Allow when no exact or pattern — existing `allowWithoutRule` path unchanged

**Placeholder scan:** None found.

**Type consistency:**
- `PatternMatcher.findBestMatch` returns `Optional<Rule>` — used correctly in `RateLimitService`
- `ruleCacheService.findPatternsByAppId` returns `List<Rule>` — matches interface and all recording stubs
- `synthesizeRule(Rule pattern, String actualClientId)` — called with correct args in Task 5
