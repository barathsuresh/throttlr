# Wildcard & Pattern Rule Matching Design

**Date:** 2026-05-12
**Status:** Approved

## Goals

Allow a single rule to cover many dynamic `clientId` values without pre-registering each one. Supports exact rules, glob-style pattern rules, and a catch-all.

## Rule Types

| `clientId` value | Type | Example match |
|---|---|---|
| `user:123` | Exact | only `user:123` |
| `user:*` | Pattern | `user:abc`, `user:xyz` |
| `user:*:free:*` | Pattern | `user:abc:free:basic` |
| `ip:10.0.*` | Pattern | `ip:10.0.1.2` |
| `*` | Catch-all | anything not matched above |

A rule is a pattern rule if its `clientId` contains `*` anywhere. Otherwise it is an exact rule.

## Match Priority

On every `POST /api/check`:

1. **Exact match** — `findByAppIdAndClientId(appId, clientId)` (existing Redis/Mongo lookup, unchanged)
2. **Best pattern match** — if exact miss, load all pattern rules for the app, test each against the incoming `clientId` using `AntPathMatcher`, pick the most specific match
3. **Allow** — if neither exact nor pattern matches, allow (existing behaviour)

**Specificity scoring** (highest wins):
- Fewest `*` tokens
- Tie-break: longer total pattern length

Example: `user:abc:free:*` (1 wildcard, length 15) beats `user:*:free:*` (2 wildcards) beats `user:*` (1 wildcard, length 6) beats `*` (1 wildcard, length 1).

## Pattern Matching Engine

Use Spring's `AntPathMatcher` configured with `:` as path separator:

```java
AntPathMatcher matcher = new AntPathMatcher(":");
matcher.match("user:*:free:*", "user:abc:free:basic"); // true
matcher.match("ip:10.0.*",     "ip:10.0.1.2");        // true
matcher.match("*",             "anything");            // true
```

`*` matches any sequence within one segment (delimited by `:`). This prevents `user:*` from accidentally matching `ip:something`.

## Counter Isolation

When a pattern rule matches a dynamic `clientId`, a synthetic `Rule` is created — copy of the matched pattern rule with `clientId` replaced by the actual incoming value. The limiter runs against the synthetic rule, so each unique caller gets an **independent Redis counter**. `user:abc` and `user:xyz` both matched by `user:*` but consume separate counters.

## Caching

### Exact rules (unchanged)
Cached at `rule:<appId>:<clientId>` with 5-minute TTL. Single key lookup on every check.

### Pattern rules (new)
Cached as a JSON list at `rule-patterns:<appId>`. Loaded once on exact miss, then all patterns evaluated in memory.

Cache invalidation for `rule-patterns:<appId>` happens when any pattern rule for that app is:
- Created
- Updated
- Deleted

If `rule-patterns:<appId>` is absent in Redis (cold miss), load all pattern rules from MongoDB (`ruleRepository.findPatternsByAppId(appId)`), cache them, then evaluate.

## Validation

On `POST /api/apps/:appId/rules` and `PUT /api/apps/:appId/rules/:clientId`:

- If `clientId` contains `*`, validate as a pattern:
  - Only `*` is a wildcard character — no other regex special chars allowed (`+`, `?`, `[`, `]`, `(`, `)`, `{`, `}`, `.`, `^`, `$`, `|`, `\`)
  - Pattern must compile and match correctly via `AntPathMatcher`
- Existing length limit (200 chars) still applies
- Uniqueness constraint unchanged — one rule per `clientId` per app

## API Surface

No new endpoints. Pattern rules are created, listed, updated, and deleted via existing rule endpoints. `clientId: "*"` and `clientId: "user:*"` are valid clientId values.

URL-encode `*` as `%2A` in path parameters:
```
PUT    /api/apps/:appId/rules/%2A           (catch-all)
PUT    /api/apps/:appId/rules/user%3A%2A    (user:*)
DELETE /api/apps/:appId/rules/%2A
```

## Files Changed

| File | Change |
|---|---|
| `service/RateLimitService.java` | Add pattern fallback: load patterns on exact miss, score + match, synthesize rule with actual clientId |
| `service/cache/RuleCacheService.java` | Add `findPatternsByAppId(appId)` and `deletePatternCache(appId)` |
| `service/cache/RedisRuleCacheService.java` | Implement pattern list cache at `rule-patterns:<appId>` |
| `repository/RuleRepository.java` | Add `findByAppIdAndClientIdContaining(appId, "*")` or equivalent query |
| `service/RuleService.java` | Validate pattern clientId on create/update; call `deletePatternCache` on create/update/delete of pattern rules |
| `service/PatternMatcher.java` (new) | Wraps `AntPathMatcher`, scores specificity, finds best match from a list |
| `service/RateLimitServiceTest.java` | Tests: exact match, pattern match, catch-all, specificity ordering, no match → allow |
| `service/RuleServiceTest.java` | Tests: valid patterns accepted, invalid patterns rejected |
| `service/PatternMatcherTest.java` (new) | Tests: scoring, match selection, `*` catch-all |

## Error Handling

| Scenario | Behaviour |
|---|---|
| `clientId` contains regex special chars but no `*` | Accepted as literal exact clientId (no change) |
| `clientId` contains `*` plus other regex chars | `400` — invalid pattern |
| Pattern rule created for appId that already has same pattern | `409` (existing conflict check) |
| Redis pattern cache unavailable | Fall back to MongoDB; check proceeds normally |

## Testing

Unit tests (no Spring context, no Redis):

- `PatternMatcherTest`: match/no-match for each pattern type; specificity ordering; `*` catch-all wins over no match; more specific prefix beats less specific
- `RateLimitServiceTest`: exact match used when present; pattern match used on exact miss; most specific pattern wins; catch-all `*` used when only wildcard matches; allow when no match
- `RuleServiceTest`: pattern with only `*` wildcards accepted; pattern with `[` or `.` rejected with 400; exact rule unaffected by pattern validation

Integration: existing 47 E2E tests must continue passing. Add smoke test for pattern rule via `/api/check`.
