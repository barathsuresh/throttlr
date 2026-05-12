# Throttlr

Throttlr is a self-serve rate limiting engine built with Spring Boot, MongoDB, Redis, JWT, and BIP39 passphrase authentication.

Redis is the hot path for runtime rate-limit decisions. MongoDB stores accounts, apps, rules, and persisted analytics snapshots. Rate-limit checks are atomic through Redis Lua scripts.

## Features

- Passphrase-based account registration and login
- JWT-protected control-plane APIs
- App creation with one-time app key display
- Rule CRUD for `FIXED_WINDOW`, `TOKEN_BUCKET`, and `SLIDING_WINDOW`
- Runtime `/api/check` endpoint authenticated by `X-App-Key`
- Temporary public demo app keys
- Redis-backed app-key and rule caching
- Redis analytics counters with scheduled MongoDB snapshot persistence
- Public endpoint abuse protection with Redis-backed `429` rate limits
- Local/prod Spring profiles with production secret validation

## Tech Stack

- Java 25
- Spring Boot 4.0.6
- MongoDB
- Redis
- JWT
- BitcoinJ BIP39 mnemonic generation
- Argon2 password hashing

## Local Quickstart

Start MongoDB and Redis locally, then run:

```bash
./mvnw spring-boot:run
```

By default, Throttlr runs with the `local` profile:

```bash
SPRING_PROFILES_ACTIVE=local
```

Local defaults are defined in:

```text
src/main/resources/application-local.yaml
```

The API runs on:

```text
http://127.0.0.1:8080
```

## Production Configuration

Use the `prod` profile for deployment:

```bash
SPRING_PROFILES_ACTIVE=prod
```

Required production environment variables:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | Enables production profile |
| `PORT` | Server port, defaults to `8080` |
| `JWT_SECRET` | JWT signing secret |
| `AUTH_LOOKUP_SECRET` | HMAC secret for passphrase lookup |
| `APP_KEY_LOOKUP_SECRET` | HMAC secret for app-key lookup |
| `ADMIN_KEY` | Reserved admin secret |
| `MONGODB_URI` | MongoDB connection URI |
| `REDIS_HOST` | Redis hostname |
| `REDIS_PORT` | Redis port, defaults to `6379` |
| `REDIS_PASSWORD` | Redis password |
| `TRUST_FORWARDED_HEADERS` | Set `true` only behind a trusted proxy |

Production startup fails if required secrets are blank or still using unsafe local placeholders.

## Authentication Model

Throttlr uses stateless JWT authentication.

1. Register creates an account and returns a one-time BIP39 passphrase.
2. The raw passphrase is never stored.
3. A lookup HMAC is stored so login can find the account.
4. An Argon2 hash is stored so the passphrase can be verified.
5. Login returns a JWT.
6. Protected control-plane routes use `Authorization: Bearer <JWT>`.
7. Runtime `/api/check` uses `X-App-Key`, not JWT.

## Route Summary

| Method | Route | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/health` | Public | Basic health check |
| `GET` | `/api/health/redis` | JWT | Redis health check |
| `GET` | `/api/health/mongo` | JWT | Mongo health check |
| `POST` | `/api/auth/register` | Public | Create account |
| `POST` | `/api/auth/login` | Public | Login with passphrase |
| `GET` | `/api/auth/me` | JWT | Current account |
| `POST` | `/api/apps` | JWT | Create app and app key |
| `GET` | `/api/apps` | JWT | List apps |
| `DELETE` | `/api/apps/{appId}` | JWT | Delete app |
| `POST` | `/api/apps/{appId}/rules` | JWT | Create rule |
| `GET` | `/api/apps/{appId}/rules` | JWT | List rules |
| `PUT` | `/api/apps/{appId}/rules/{clientId}` | JWT | Update rule |
| `DELETE` | `/api/apps/{appId}/rules/{clientId}` | JWT | Delete rule |
| `POST` | `/api/check` | App key | Runtime rate-limit decision |
| `POST` | `/api/demo/app-key` | Public | Create temporary demo app key |
| `GET` | `/api/apps/{appId}/analytics` | JWT | Current-hour analytics |

## Auth API

### Register

```http
POST /api/auth/register
```

Response `201`:

```json
{
  "accountId": "69ffdfa887ff228bc238410b",
  "passphrase": "twelve bip39 words shown once",
  "message": "Save this passphrase now because it will not be shown again."
}
```

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "passphrase": "twelve bip39 words"
}
```

Response `200`:

```json
{
  "sessionToken": "eyJhbGciOi...",
  "accountId": "69ffdfa887ff228bc238410b"
}
```

### Me

```http
GET /api/auth/me
Authorization: Bearer <JWT>
```

Response `200`:

```json
{
  "accountId": "69ffdfa887ff228bc238410b"
}
```

## Apps API

### Create App

```http
POST /api/apps
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "Production API"
}
```

Response `201`:

```json
{
  "appId": "69ffdfa887ff228bc238410c",
  "name": "Production API",
  "appKey": "throttlr_live_...",
  "message": "App created successfully. Save this app key now because it will not be shown again."
}
```

The app key is shown once. Store it securely.

### List Apps

```http
GET /api/apps?page=0&size=10
Authorization: Bearer <JWT>
```

Response `200`:

```json
{
  "items": [
    {
      "appId": "69ffdfa887ff228bc238410c",
      "name": "Production API",
      "ruleCount": 1,
      "createdAt": 1778374800000
    }
  ],
  "page": 0,
  "size": 10,
  "totalItems": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

### Delete App

```http
DELETE /api/apps/{appId}
Authorization: Bearer <JWT>
```

Response `204`.

## Rules API

Supported algorithms:

```text
FIXED_WINDOW
TOKEN_BUCKET
SLIDING_WINDOW
```

### Create Rule

```http
POST /api/apps/{appId}/rules
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "clientId": "user:123",
  "algorithm": "FIXED_WINDOW",
  "limitPerWindow": 100,
  "windowMs": 60000
}
```

Response `201`:

```json
{
  "ruleId": "69efae0b253f86c77f2e3594",
  "clientId": "user:123",
  "algorithm": "FIXED_WINDOW",
  "limitPerWindow": 100,
  "windowMs": 60000
}
```

`clientId` is your identifier for the thing being limited. Examples: `user:123`, `ip:203.0.113.10`, `team:free-tier`.

### List Rules

```http
GET /api/apps/{appId}/rules?page=0&size=10
Authorization: Bearer <JWT>
```

Response `200`:

```json
{
  "items": [
    {
      "ruleId": "69efae0b253f86c77f2e3594",
      "clientId": "user:123",
      "algorithm": "FIXED_WINDOW",
      "limitPerWindow": 100,
      "windowMs": 60000
    }
  ],
  "page": 0,
  "size": 10,
  "totalItems": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

### Update Rule

```http
PUT /api/apps/{appId}/rules/{clientId}
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "clientId": "user:123",
  "algorithm": "TOKEN_BUCKET",
  "limitPerWindow": 50,
  "windowMs": 60000
}
```

Response `200` returns the updated rule.

### Delete Rule

```http
DELETE /api/apps/{appId}/rules/{clientId}
Authorization: Bearer <JWT>
```

Response `204`.

## Runtime Check API

```http
POST /api/check
X-App-Key: throttlr_live_...
Content-Type: application/json

{
  "clientId": "user:123"
}
```

Response `200`:

```json
{
  "allowed": true,
  "remaining": 99,
  "resetAfterMs": 60000,
  "retryAfterMs": 0
}
```

If blocked:

```json
{
  "allowed": false,
  "remaining": 0,
  "resetAfterMs": 59930,
  "retryAfterMs": 59930
}
```

If no matching rule exists, Throttlr currently allows the request.

## Demo API

```http
POST /api/demo/app-key
```

Response `201`:

```json
{
  "appId": "demo-9449c9ca-4523-4694-9107-ab582ff0ce39",
  "appKey": "throttlr_live_...",
  "expiresInMs": 900000,
  "rules": [
    { "clientId": "demo-user-fixed-window",   "algorithm": "FIXED_WINDOW",   "limitPerWindow": 10, "windowMs": 60000 },
    { "clientId": "demo-user-token-bucket",   "algorithm": "TOKEN_BUCKET",   "limitPerWindow": 10, "windowMs": 60000 },
    { "clientId": "demo-user-sliding-window", "algorithm": "SLIDING_WINDOW", "limitPerWindow": 10, "windowMs": 60000 }
  ],
  "message": "Temporary demo app key created. It expires automatically."
}
```

Use the returned `appKey` with any `clientId` from `rules` to test that algorithm via `/api/check`.

## Analytics API

```http
GET /api/apps/{appId}/analytics
Authorization: Bearer <JWT>
```

Response `200`:

```json
{
  "appId": "69ffdfa887ff228bc238410c",
  "hour": 1778374800000,
  "total": 10,
  "allowed": 7,
  "blocked": 3
}
```

Analytics are counted in Redis on the runtime path. A scheduled job snapshots hourly counters into MongoDB. Old analytics documents are cleaned up based on `app.analytics.retention-days`.

Default analytics config:

```yaml
app:
  analytics:
    flush-cron: "0 0 * * * *"
    cleanup-cron: "0 30 2 * * *"
    retention-days: 30
```

## Error Format

Most application errors use:

```json
{
  "message": "App not found",
  "status": 404,
  "timestamp": 1778374800000
}
```

Common statuses:

| Status | Meaning |
| --- | --- |
| `400` | Invalid request input |
| `401` | Missing or invalid auth |
| `404` | Resource not found or not owned by account |
| `409` | Duplicate/conflicting resource |
| `429` | Public endpoint rate limit exceeded |
| `500` | Internal server error |

## Public Endpoint Abuse Limits

These public endpoints are IP-rate-limited with Redis:

| Route | Limit |
| --- | --- |
| `POST /api/auth/register` | `5` requests per `10` minutes |
| `POST /api/auth/login` | `10` requests per `1` minute |
| `POST /api/demo/app-key` | `3` requests per `10` minutes |

When exceeded:

```json
{
  "message": "Too many requests. Please try again later.",
  "status": 429,
  "timestamp": 1778374800000
}
```

The response also includes a `Retry-After` header.

## Cache Behavior

Redis caches:

- App-key lookup results for runtime authentication
- Rule lookup results for `/api/check`
- Runtime limiter counters
- Runtime analytics counters

Cache invalidation happens when:

- An app is deleted
- A rule is created
- A rule is updated
- A rule is deleted

MongoDB remains off the hot path except cache misses and scheduled analytics persistence.

## Security Notes

- JWT auth is stateless.
- App keys are shown once and stored only as hash/lookup material.
- Passphrases are shown once and stored only as lookup HMAC plus Argon2 hash.
- Production profile requires real secrets.
- `X-Forwarded-For` is ignored by default.
- Set `TRUST_FORWARDED_HEADERS=true` only behind a trusted proxy.
- Detailed health endpoints require JWT.

## Example Curl Flow

Register:

```bash
curl -sS -X POST http://127.0.0.1:8080/api/auth/register
```

Login:

```bash
curl -sS -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"passphrase":"<passphrase>"}'
```

Create app:

```bash
curl -sS -X POST http://127.0.0.1:8080/api/apps \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"CLI Test App"}'
```

Create rule:

```bash
curl -sS -X POST http://127.0.0.1:8080/api/apps/<appId>/rules \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"user:123","algorithm":"FIXED_WINDOW","limitPerWindow":2,"windowMs":60000}'
```

Check:

```bash
curl -sS -X POST http://127.0.0.1:8080/api/check \
  -H 'X-App-Key: <appKey>' \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"user:123"}'
```

## Verification

Recent full E2E smoke testing covered:

- Health endpoints
- Auth register/login/me
- App creation/list validation
- Rule create/list/update/delete validation
- Fixed-window runtime limiting
- Token-bucket runtime limiting
- Sliding-window runtime limiting
- Demo app keys
- Analytics read API
- Public endpoint abuse limits
