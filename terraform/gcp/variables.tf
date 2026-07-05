variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region for Cloud Run and Artifact Registry"
  type        = string
  default     = "us-central1"
}

variable "service_name" {
  description = "Cloud Run service name"
  type        = string
  default     = "throttlr-api"
}

variable "artifact_repository" {
  description = "Artifact Registry repository name"
  type        = string
  default     = "throttlr"
}

variable "image" {
  description = "Full container image reference to deploy (e.g. us-central1-docker.pkg.dev/PROJECT/throttlr/throttlr-api:TAG)"
  type        = string
}

variable "allow_unauthenticated" {
  description = "Allow public (unauthenticated) access to the Cloud Run service"
  type        = bool
  default     = true
}

# --- Runtime shape (matches the benchmarked configuration) ---

variable "cpu" {
  description = "CPU per instance"
  type        = string
  default     = "2"
}

variable "memory" {
  description = "Memory per instance"
  type        = string
  default     = "2Gi"
}

variable "concurrency" {
  description = "Max concurrent requests per instance"
  type        = number
  default     = 5
}

variable "min_instances" {
  description = "Minimum number of instances"
  type        = number
  default     = 0
}

variable "max_instances" {
  description = "Maximum number of instances"
  type        = number
  default     = 40
}

# --- Application secrets (values land in Secret Manager) ---

variable "jwt_secret" {
  description = "HMAC-SHA256 signing key for JWTs (>= 32 bytes)"
  type        = string
  sensitive   = true
}

variable "auth_lookup_secret" {
  description = "HMAC key for passphrase lookup index"
  type        = string
  sensitive   = true
}

variable "app_key_lookup_secret" {
  description = "HMAC key for app-key lookup index"
  type        = string
  sensitive   = true
}

variable "admin_key" {
  description = "Admin API secret"
  type        = string
  sensitive   = true
}

variable "mongodb_uri" {
  description = "Full MongoDB connection URI (e.g. MongoDB Atlas)"
  type        = string
  sensitive   = true
}

variable "redis_host" {
  description = "Redis hostname (e.g. Upstash endpoint)"
  type        = string
  sensitive   = true
}

variable "redis_port" {
  description = "Redis port"
  type        = string
  default     = "6379"
  sensitive   = true
}

variable "redis_password" {
  description = "Redis authentication password"
  type        = string
  sensitive   = true
}

variable "cors_allowed_origins" {
  description = "Comma-separated allowed CORS origins"
  type        = string
  sensitive   = true
}
