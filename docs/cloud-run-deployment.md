# Cloud Run Deployment

This backend is prepared for Cloud Run with:

- `Dockerfile`
- `SPRING_PROFILES_ACTIVE=prod`
- MongoDB Atlas via `MONGODB_URI`
- Redis over TLS via `REDIS_SSL_ENABLED=true`
- GitHub Actions deployment to Cloud Run

## Recommended Services

- Backend: Google Cloud Run
- MongoDB: MongoDB Atlas M0 or paid cluster
- Redis: Upstash Redis for simple public TLS access, or GCP Memorystore later

## Required GitHub Variables

Configure these under GitHub repository settings:

| Variable | Example |
| --- | --- |
| `GCP_PROJECT_ID` | `throttlr-prod` |
| `GCP_REGION` | `us-central1` |
| `CLOUD_RUN_SERVICE` | `throttlr-api` |
| `ARTIFACT_REGISTRY_REPOSITORY` | `throttlr` |

## Required GitHub Secrets

For Workload Identity Federation:

| Secret | Purpose |
| --- | --- |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Workload identity provider resource name |
| `GCP_SERVICE_ACCOUNT` | Deploy service account email |

Runtime secrets are expected in Google Secret Manager:

| Secret Manager Secret | Runtime Env Var |
| --- | --- |
| `JWT_SECRET` | `JWT_SECRET` |
| `AUTH_LOOKUP_SECRET` | `AUTH_LOOKUP_SECRET` |
| `APP_KEY_LOOKUP_SECRET` | `APP_KEY_LOOKUP_SECRET` |
| `ADMIN_KEY` | `ADMIN_KEY` |
| `MONGODB_URI` | `MONGODB_URI` |
| `REDIS_HOST` | `REDIS_HOST` |
| `REDIS_PORT` | `REDIS_PORT` |
| `REDIS_PASSWORD` | `REDIS_PASSWORD` |
| `CORS_ALLOWED_ORIGINS` | `CORS_ALLOWED_ORIGINS` |

## Local Docker Test

```bash
docker build -t throttlr-api .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET="replace-me" \
  -e AUTH_LOOKUP_SECRET="replace-me" \
  -e APP_KEY_LOOKUP_SECRET="replace-me" \
  -e ADMIN_KEY="replace-me" \
  -e MONGODB_URI="replace-me" \
  -e REDIS_HOST="replace-me" \
  -e REDIS_PORT="6379" \
  -e REDIS_PASSWORD="replace-me" \
  -e REDIS_SSL_ENABLED="true" \
  -e CORS_ALLOWED_ORIGINS="https://your-frontend.example" \
  throttlr-api
```

Use real strong secrets. Blank values and the known local-development defaults are rejected in the `prod` profile.
