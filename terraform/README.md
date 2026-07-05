# Terraform

Standalone infrastructure-as-code for deploying the Throttlr backend. **Not wired into CI/CD** — the GitHub Actions workflows are unchanged and continue to deploy the existing environment. Use these stacks manually, from your machine.

Two independent stacks (pick one, or both for separate environments):

| Stack | Compute | Registry | Secrets |
|---|---|---|---|
| [`gcp/`](gcp/) | Cloud Run | Artifact Registry | Secret Manager |
| [`aws/`](aws/) | App Runner | ECR | Secrets Manager |

Both mirror the production runtime described in the README: `prod` Spring profile, benchmarked scaling shape (concurrency 5, max 40 instances), all secrets injected from the cloud secret store — never baked into the image or plain env vars.

MongoDB and Redis are **not** provisioned here. The app is designed for external managed services (MongoDB Atlas, Upstash Redis) — supply their connection details as secret variables.

## Prerequisites

- Terraform >= 1.6
- Docker (to build/push the image)
- GCP: `gcloud` authenticated (`gcloud auth application-default login`)
- AWS: credentials configured (`aws configure` or env vars)

## Usage (either stack)

```bash
cd terraform/gcp   # or terraform/aws
cp terraform.tfvars.example terraform.tfvars
# edit terraform.tfvars — fill in project/region and real secrets
terraform init
terraform plan
terraform apply
```

`terraform.tfvars` holds secrets and is gitignored. Never commit it. For teams, prefer passing secrets via `TF_VAR_*` environment variables or a secrets-injection tool instead of a file on disk.

## Two-phase bootstrap (first deploy)

Cloud Run and App Runner both require the container image to exist before the service can be created, but the registry itself is part of the stack. On a fresh environment:

1. Create the registry only:
   ```bash
   # GCP
   terraform apply -target=google_artifact_registry_repository.throttlr
   # AWS
   terraform apply -target=aws_ecr_repository.throttlr
   ```
2. Build and push the image:
   ```bash
   # GCP
   gcloud auth configure-docker REGION-docker.pkg.dev
   docker buildx build --platform linux/amd64 \
     -t REGION-docker.pkg.dev/PROJECT/throttlr/throttlr-api:latest --push .

   # AWS
   aws ecr get-login-password --region REGION | \
     docker login --username AWS --password-stdin ACCOUNT.dkr.ecr.REGION.amazonaws.com
   docker buildx build --platform linux/amd64 \
     -t ACCOUNT.dkr.ecr.REGION.amazonaws.com/throttlr-api:latest --push .
   ```
3. Full apply:
   ```bash
   terraform apply
   ```

## Adopting the existing GCP deployment

The backend is already live on Cloud Run, created outside Terraform. Applying `gcp/` against the same project will fail with "already exists" errors on the Secret Manager secrets, Artifact Registry repo, and Cloud Run service. Either:

- **Separate environment (recommended):** use a different `project_id` (or change `service_name`/`artifact_repository`/secret names) and leave the live deployment untouched, or
- **Import:** bring existing resources under Terraform management, e.g.
  ```bash
  terraform import 'google_artifact_registry_repository.throttlr' \
    projects/PROJECT/locations/REGION/repositories/throttlr
  terraform import 'google_cloud_run_v2_service.throttlr' \
    projects/PROJECT/locations/REGION/services/throttlr-api
  terraform import 'google_secret_manager_secret.app["JWT_SECRET"]' \
    projects/PROJECT/secrets/JWT_SECRET
  # ...repeat for each secret
  ```

## GCP vs AWS differences

- **Memory:** Cloud Run runs 2 CPU / 2 Gi; App Runner's smallest memory pairing for 2 vCPU is 4 GB.
- **Scale to zero:** Cloud Run supports `min_instances = 0`; App Runner's minimum is 1 running instance (it pauses billing for compute when idle, but the instance floor is 1).
- **Health checks:** App Runner probes `/api/health` explicitly; Cloud Run uses its default startup/liveness behavior, matching the current deployment.
- **State:** both stacks default to local state (`terraform.tfstate`, gitignored). For anything shared, configure a remote backend (GCS / S3 + DynamoDB) in `versions.tf`.

## Secret rotation

Change the value in `terraform.tfvars` (or the `TF_VAR_*` env var) and `terraform apply`. A new secret version is created and the service picks it up on the next revision/deployment.
