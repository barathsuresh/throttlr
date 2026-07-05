# --- Required APIs ---

resource "google_project_service" "apis" {
  for_each = toset([
    "run.googleapis.com",
    "artifactregistry.googleapis.com",
    "secretmanager.googleapis.com",
  ])

  service            = each.value
  disable_on_destroy = false
}

# --- Artifact Registry ---

resource "google_artifact_registry_repository" "throttlr" {
  location      = var.region
  repository_id = var.artifact_repository
  format        = "DOCKER"
  description   = "Throttlr container images"

  depends_on = [google_project_service.apis]
}

# --- Runtime service account ---

resource "google_service_account" "runtime" {
  account_id   = "${var.service_name}-runtime"
  display_name = "Throttlr Cloud Run runtime"
}

# --- Secret Manager ---

locals {
  secrets = {
    JWT_SECRET            = var.jwt_secret
    AUTH_LOOKUP_SECRET    = var.auth_lookup_secret
    APP_KEY_LOOKUP_SECRET = var.app_key_lookup_secret
    ADMIN_KEY             = var.admin_key
    MONGODB_URI           = var.mongodb_uri
    REDIS_HOST            = var.redis_host
    REDIS_PORT            = var.redis_port
    REDIS_PASSWORD        = var.redis_password
    CORS_ALLOWED_ORIGINS  = var.cors_allowed_origins
  }
}

resource "google_secret_manager_secret" "app" {
  for_each = local.secrets

  secret_id = each.key

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "app" {
  for_each = local.secrets

  secret      = google_secret_manager_secret.app[each.key].id
  secret_data = each.value
}

resource "google_secret_manager_secret_iam_member" "runtime_access" {
  for_each = local.secrets

  secret_id = google_secret_manager_secret.app[each.key].id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.runtime.email}"
}

# --- Cloud Run ---

resource "google_cloud_run_v2_service" "throttlr" {
  name                = var.service_name
  location            = var.region
  deletion_protection = false

  template {
    service_account                  = google_service_account.runtime.email
    max_instance_request_concurrency = var.concurrency

    scaling {
      min_instance_count = var.min_instances
      max_instance_count = var.max_instances
    }

    containers {
      image = var.image

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = var.cpu
          memory = var.memory
        }
        startup_cpu_boost = true
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
      env {
        name  = "TRUST_FORWARDED_HEADERS"
        value = "true"
      }
      env {
        name  = "REDIS_SSL_ENABLED"
        value = "true"
      }

      dynamic "env" {
        for_each = local.secrets
        content {
          name = env.key
          value_source {
            secret_key_ref {
              secret  = google_secret_manager_secret.app[env.key].secret_id
              version = "latest"
            }
          }
        }
      }
    }
  }

  depends_on = [
    google_project_service.apis,
    google_secret_manager_secret_version.app,
    google_secret_manager_secret_iam_member.runtime_access,
  ]
}

resource "google_cloud_run_v2_service_iam_member" "public" {
  count = var.allow_unauthenticated ? 1 : 0

  name     = google_cloud_run_v2_service.throttlr.name
  location = var.region
  role     = "roles/run.invoker"
  member   = "allUsers"
}
