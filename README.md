# Throttlr

**Self-hosted rate-limiting API for backend engineers who want Redis-backed enforcement, multiple algorithms, and a full control plane — without paying per-seat for a third-party gateway.**

![rate-limiting](https://img.shields.io/badge/topic-rate--limiting-blue)
![spring-boot](https://img.shields.io/badge/topic-spring--boot-blue)
![redis](https://img.shields.io/badge/topic-redis-blue)
![java](https://img.shields.io/badge/topic-java-blue)
![gcp](https://img.shields.io/badge/topic-gcp-blue)

---

## Contents

- [Architecture](#architecture)
- [Algorithms](#algorithms)
- [Performance](#performance)
- [Security Design](#security-design)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Configuration](#configuration)

---

## Architecture

Throttlr has two distinct planes: a **data plane** (`POST /api/check`) evaluated on every request from your services, and a **control plane** (apps, rules, analytics) protected by JWT issued at login.

### Data plane flow

1. A client service calls `POST /api/check` with the `X-App-Key` header.
2. The check endpoint forwards the request to `RateLimitService`.
3. The service looks up the rule in Redis; on a cache miss, it loads the rule from MongoDB.
4. A Redis Lua script performs the atomic rate-limit decision.
5. The response returns `allowed`, `remaining`, and `retryAfter` values.
6. The hit or block is recorded in Redis analytics.
7. An hourly cron job flushes Redis analytics into MongoDB.

### Control plane flow

1. The dashboard or API client logs in through `POST /api/auth/login`.
2. Login returns a JWT.
3. The JWT is used for apps CRUD at `/api/apps`.
4. The JWT is used for rules CRUD at `/api/apps/{id}/rules`.
5. Apps, rules, and account data are stored in MongoDB.

---

## Algorithms

All three algorithms are implemented as atomic Redis Lua scripts. Algorithm is configured per-rule.

| Algorithm | How it works | Best use case |
|---|---|---|
| **Fixed Window** | Counter increments in a fixed time slot; resets when the window boundary rolls over. Cheapest storage: one Redis key per client. | High-volume, cost-sensitive workloads where boundary bursts are acceptable. |
| **Token Bucket** | Bucket refills continuously at `limit / windowMs` tokens/ms. Requests consume one token; denied when bucket is empty. Stored as a Redis hash (token count + timestamp). | APIs where steady throughput matters more than strict burst control. |
| **Sliding Window** | Timestamps of all requests stored in a Redis sorted set; stale entries pruned on each check. Most accurate — no boundary burst possible. | Per-user or per-key limits where fairness is critical. |

> **Memory note:** Sliding window stores one sorted-set entry per request within the window. At high request rates or wide windows, memory cost grows linearly. Fixed window and token bucket each use O(1) storage per client regardless of request volume.

---

## Performance

Load test: 5,000 `POST /api/check` requests, 20 concurrent clients, deployed on GCP Cloud Run.

| Metric | Result |
|---|---|
| Throughput | 44.9 req/sec |
| p50 latency | 551 ms |
| p95 latency | 625 ms |
| p99 latency | 1,012 ms |
| Error rate | 0% |

Baseline throughput before Cloud Run tuning was 9.77 req/sec. Tuning Cloud Run concurrency, min instances, and enabling Java virtual threads achieved a **4.6× throughput improvement** with no code changes to the hot path.

---

## Security Design

### Authentication model

Accounts have no password. Registration returns a **BIP39 12-word passphrase** shown exactly once — it is never stored in plaintext. Internally:

- An HMAC-SHA256 digest of the passphrase (keyed with `AUTH_LOOKUP_SECRET`) is stored as the lookup index. This lets the database find the account in O(1) without exposing the raw passphrase.
- An Argon2 hash of the passphrase is stored for verification. Argon2 is memory-hard and resistant to GPU/ASIC cracking.

Login verifies the passphrase through the same two-step process and returns a JWT (HMAC-SHA256 signed, 24-hour expiry).

### App keys

App keys (`throttlr_live_...`) follow the same two-layer pattern: an HMAC lookup hash for fast resolution, and an Argon2 hash for verification. The raw key is shown to the operator once at creation and never stored.

### Startup validation

`ProductionSecretValidator` runs at startup in the `prod` profile. It fails immediately if any required secret (`JWT_SECRET`, `AUTH_LOOKUP_SECRET`, `APP_KEY_LOOKUP_SECRET`, `ADMIN_KEY`) is blank or still set to a known local placeholder. The application will not start in production with insecure defaults.

### IP handling

`X-Forwarded-For` is **ignored by default**. Set `TRUST_FORWARDED_HEADERS=true` only when running behind a trusted proxy (e.g., a load balancer that overwrites the header). Client IP addresses are SHA-256 hashed before use as Redis key components — raw IPs are never stored in Redis.

### JWT scope

JWTs are used only on the control plane (app/rule management, analytics). The data-plane check endpoint (`POST /api/check`) uses `X-App-Key` header authentication instead. Public endpoints (`/api/auth/register`, `/api/auth/login`, `/api/demo/app-key`) are rate-limited by IP using a separate fixed-window Lua script.

---

## Quick Start

**Prerequisites:** Java 25, Docker (for Redis + MongoDB), Maven.

1. Clone the repo and start dependencies:
   ```
   docker-compose up -d
   ```

2. Run the application:
   ```
   ./mvnw spring-boot:run
   ```

The server starts on `http://localhost:8080`. The local profile uses permissive placeholder secrets; `ProductionSecretValidator` is inactive in `local` profile.

To exercise the API locally: register an account (save the passphrase), log in to get a JWT, create an app (save the app key), create a rule, then call `POST /api/check` with `X-App-Key`.

---

## API Reference

### Data plane

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/check` | `X-App-Key` | Evaluate rate limit for a client ID |

### Control plane — auth

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | None | Create account; returns one-time passphrase |
| `POST` | `/api/auth/login` | None | Authenticate; returns JWT |
| `GET` | `/api/auth/me` | JWT | Return current account ID |

### Control plane — apps

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/apps` | JWT | Create app; returns one-time app key |
| `GET` | `/api/apps` | JWT | List apps (paginated) |
| `DELETE` | `/api/apps/{appId}` | JWT | Delete app |
| `GET` | `/api/apps/{appId}/analytics` | JWT | Hourly hit/block counts |

### Control plane — rules

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/apps/{appId}/rules` | JWT | Create rule for a client ID or glob pattern |
| `GET` | `/api/apps/{appId}/rules` | JWT | List rules (paginated) |
| `PUT` | `/api/apps/{appId}/rules/{clientId}` | JWT | Update rule |
| `DELETE` | `/api/apps/{appId}/rules/{clientId}` | JWT | Delete rule |

### Utility

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/health` | None | Application health |
| `GET` | `/api/health/redis` | JWT | Redis connectivity |
| `GET` | `/api/health/mongo` | JWT | MongoDB connectivity |
| `POST` | `/api/demo/app-key` | None | Generate ephemeral demo app (15-min TTL) |

**`POST /api/check` request body:**
```json
{
  "clientId": "user-123",
  "metadata": {}
}
```

**`CheckResponse`:**
```json
{
  "allowed": true,
  "remaining": 47,
  "resetAfterMs": 38000,
  "retryAfterMs": null
}
```

---

## Configuration

All secrets must be injected as environment variables. The `prod` profile rejects any missing or placeholder value at startup.

| Variable | Required | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | Set to `prod` |
| `JWT_SECRET` | Yes | HMAC-SHA256 signing key for JWTs (≥32 bytes) |
| `AUTH_LOOKUP_SECRET` | Yes | HMAC key for passphrase lookup index |
| `APP_KEY_LOOKUP_SECRET` | Yes | HMAC key for app-key lookup index |
| `ADMIN_KEY` | Yes | Admin API secret |
| `MONGODB_URI` | Yes | Full MongoDB connection URI |
| `REDIS_HOST` | Yes | Redis hostname |
| `REDIS_PORT` | No | Redis port (default: `6379`) |
| `REDIS_PASSWORD` | Yes | Redis authentication password |
| `REDIS_SSL_ENABLED` | No | Enable TLS for Redis (default: `true`) |
| `PORT` | No | HTTP port (default: `8080`) |
| `TRUST_FORWARDED_HEADERS` | No | Trust `X-Forwarded-For` (default: `false`) |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated allowed origins |

Scheduling and cache TTLs can be overridden via `app.analytics.flush-cron`, `app.analytics.cleanup-cron`, `app.analytics.retention-days`, and `app.cache.rule-ttl-ms`.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 25, virtual threads |
| Framework | Spring Boot 4.x |
| Enforcement | Redis + Lua (atomic scripts) |
| Persistence | MongoDB |
| Auth | JWT (jjwt), Argon2, HMAC-SHA256 |
| Passphrase | BIP39 via bitcoinj |
| Deploy | GCP Cloud Run (Dockerfile included) |

---

## License

MIT — see [LICENSE](LICENSE).
