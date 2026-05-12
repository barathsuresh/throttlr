# Throttlr Frontend Design

**Date:** 2026-05-11
**Status:** Approved

## Goals

Build a React + TypeScript frontend for Throttlr that serves two purposes:
1. Functional dashboard — manage apps, rules, and analytics
2. Portfolio showcase — polished enough to demo publicly to recruiters and employers

## Stack

- **React + TypeScript** — Vite scaffold
- **React Router v6** — client-side routing
- **TanStack Query (React Query)** — server state, caching, pagination
- **Tailwind CSS + shadcn/ui** — styling and components
- **Axios** — HTTP client with request/response interceptors
- **Location:** `/frontend` subdirectory in this repo

## Project Structure

```
frontend/
├── src/
│   ├── api/
│   │   ├── axios.ts              # axios instance + interceptors
│   │   ├── auth.ts               # register, login, me
│   │   ├── apps.ts               # createApp, listApps, deleteApp
│   │   ├── rules.ts              # createRule, listRules, updateRule, deleteRule
│   │   ├── analytics.ts          # getAnalytics
│   │   ├── check.ts              # runtime check
│   │   └── demo.ts               # createDemoAppKey
│   ├── components/
│   │   ├── ui/                   # shadcn generated components
│   │   ├── BackendHealthBanner.tsx
│   │   ├── OnetimeSecretModal.tsx
│   │   ├── AppCard.tsx
│   │   ├── RuleForm.tsx
│   │   └── AnalyticsPanel.tsx
│   ├── hooks/
│   │   ├── useBackendHealth.ts
│   │   ├── useAuth.ts
│   │   ├── useApps.ts
│   │   ├── useRules.ts
│   │   └── useAnalytics.ts
│   ├── pages/
│   │   ├── Landing.tsx
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   ├── Dashboard.tsx
│   │   ├── AppRules.tsx
│   │   └── Demo.tsx
│   ├── lib/
│   │   ├── queryClient.ts
│   │   └── token.ts              # get/set/clear JWT in localStorage
│   └── router.tsx
├── .env.local                    # VITE_API_URL=http://127.0.0.1:8080
├── index.html
├── vite.config.ts
├── tailwind.config.ts
└── package.json
```

## Routing

| Route | Access | Page |
|---|---|---|
| `/` | Public | Landing |
| `/demo` | Public | Demo playground |
| `/login` | Public (redirect if authed) | Login |
| `/register` | Public (redirect if authed) | Register |
| `/dashboard` | Protected | Apps list + analytics |
| `/apps/:appId/rules` | Protected | Rules CRUD |

- `ProtectedRoute` wrapper: no token → redirect `/login`
- Auth routes: has token → redirect `/dashboard`

## Auth Flow

1. `POST /api/auth/register` → `OnetimeSecretModal` shows passphrase with copy button + "I've saved this" confirmation gate before navigating to `/dashboard`
2. `POST /api/auth/login` → JWT stored in `localStorage` via `token.ts`
3. Every axios request attaches `Authorization: Bearer <token>` via request interceptor
4. `401` response → clear token + redirect `/login` via response interceptor
5. `POST /api/apps` (create app) → `OnetimeSecretModal` shows app key with same copy + confirm pattern

`OnetimeSecretModal` is reused for both passphrase and app key — no way to dismiss without clicking the confirmation button.

## React Query Hooks

| Hook | Query key | Endpoint | Notes |
|---|---|---|---|
| `useBackendHealth()` | `['health']` | `GET /api/health` | Single check on mount; only polls every 3s if initial check fails; stops on first 200 |
| `useApps(page)` | `['apps', page]` | `GET /api/apps?page&size=10` | |
| `useRules(appId, page)` | `['rules', appId, page]` | `GET /api/apps/:id/rules` | |
| `useAnalytics(appId)` | `['analytics', appId]` | `GET /api/apps/:id/analytics` | |

Mutations invalidate on success:
- `createApp`, `deleteApp` → invalidate `['apps']`
- `createRule`, `updateRule`, `deleteRule` → invalidate `['rules', appId]`

## API Layer

Single axios instance in `api/axios.ts`:
- `baseURL` from `VITE_API_URL` env var
- Request interceptor: attach `Authorization: Bearer <token>`
- Response interceptor: `401` → `token.clear()` + `navigate('/login')`

Typed API functions in resource files return typed response objects matching backend DTOs.

## Pages

### `/` Landing
Minimal hero. Tagline and short description of Throttlr. Two CTAs: "Try Demo" and "Login". Tech stack badges (Spring Boot, Redis, MongoDB, JWT). No auth required.

### `/register`
Single submit button — no username or password, matches backend's passphrase model. On 201, `OnetimeSecretModal` displays the BIP39 passphrase with copy-to-clipboard. User must click "I've saved my passphrase" to proceed to `/dashboard`. Modal cannot be dismissed before confirming.

### `/login`
Single textarea for passphrase. On 200, store JWT, redirect to `/dashboard`.

### `/dashboard`
Paginated app list. Each `AppCard` shows: name, rule count, created date. Actions:
- "Create App" → name input → POST → `OnetimeSecretModal` for app key → app list refetches
- "Delete App" → confirmation dialog → DELETE → list refetches
- Expandable `AnalyticsPanel` per app showing `total`, `allowed`, `blocked` for the current hour

### `/apps/:appId/rules`
Breadcrumb back to `/dashboard`. Paginated rules table: clientId, algorithm badge, limitPerWindow, windowMs. Actions:
- "Add Rule" → inline `RuleForm`: clientId, algorithm select (FIXED_WINDOW / TOKEN_BUCKET / SLIDING_WINDOW), limitPerWindow, windowMs
- Edit rule in-place → PUT → list refetches
- Delete rule → confirmation → DELETE → list refetches

### `/demo`
`useBackendHealth` gate — backend cold → show wake-up banner, all buttons disabled. On backend alive:
1. Single `POST /api/demo/app-key` on mount
2. Response contains one `appKey` and three rules (one per algorithm, each with its own `clientId`)
3. Three tabs: **Fixed Window**, **Token Bucket**, **Sliding Window**
4. Each tab shows: algorithm name, `clientId`, `limitPerWindow`, `windowMs`, expiry countdown
5. "Send Request" button → POST `/api/check` with tab's `clientId` and shared `appKey`
6. Live display: `allowed` badge (green/red), `remaining`, `resetAfterMs`
7. Request history list — last 10 calls per tab

## Cold-Start Handling

`useBackendHealth` makes a single `GET /api/health` on mount. Behaviour:
- If initial check returns 200 → backend alive, no banner shown, no further polling (warm path = 1 request total)
- If initial check fails → `BackendHealthBanner` renders at top of every page with spinner: "Backend is waking up on Cloud Run…" and polling begins every 3s
- Polling stops immediately on first 200 — banner auto-dismisses
- All React Query data hooks use `enabled: isBackendAlive` — no API calls fire until health returns 200
- Applies to all pages including `/demo` — public users hit this cold most often

```ts
useQuery({
  queryKey: ['health'],
  queryFn: fetchHealth,
  retry: false,
  refetchInterval: (query) =>
    query.state.status === 'success' ? false : 3000,
})
```

## Error Handling

| Scenario | Behaviour |
|---|---|
| `401` | Clear token, redirect `/login` |
| `409` | Inline form error below the relevant field |
| `429` | Toast with `Retry-After` seconds displayed |
| `400` | Inline form error or toast with `message` from response body |
| `404` | Toast "Not found" |
| `5xx` / network error | Toast "Something went wrong, try again" |

Toast notifications via shadcn `<Sonner>`.

## Dev Setup

```bash
# Start backend (from repo root)
./mvnw spring-boot:run          # runs on localhost:8080

# Start frontend
cd frontend
npm install
npm run dev                     # runs on localhost:5173
```

`frontend/.env.local`:
```
VITE_API_URL=http://127.0.0.1:8080
```

For production builds, `VITE_API_URL` is set to the Cloud Run URL at build time. CORS is already configured in Spring Boot to allow `localhost:5173`.

## Backend Changes Already Made

The demo endpoint was extended before frontend work began:

- `DemoAppResponse` now returns `appKey`, `expiresInMs`, and a `rules[]` array instead of flat fields
- `rules[]` contains three entries — one each for `FIXED_WINDOW`, `TOKEN_BUCKET`, `SLIDING_WINDOW` — each with `clientId`, `algorithm`, `limitPerWindow`, `windowMs`
- `DemoService` creates one demo app with three rules (unique `clientId` per algorithm), all cached in Redis with TTL
- Single API call on demo page mount; avoids exhausting the `3 req / 10 min` public rate limit

All 47 local E2E tests pass against the updated backend.
