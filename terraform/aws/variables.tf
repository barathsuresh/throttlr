variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "service_name" {
  description = "App Runner service name (also used to prefix ECR/IAM/secret names)"
  type        = string
  default     = "throttlr-api"
}

variable "image_tag" {
  description = "Tag of the image in ECR to deploy"
  type        = string
  default     = "latest"
}

# --- Runtime shape ---
# App Runner pairs 2 vCPU with a minimum of 4 GB; this is the closest match
# to the benchmarked Cloud Run shape (2 CPU / 2Gi / concurrency 5 / max 40).

variable "cpu" {
  description = "vCPU per instance (App Runner units, e.g. \"2 vCPU\")"
  type        = string
  default     = "2 vCPU"
}

variable "memory" {
  description = "Memory per instance (App Runner units, e.g. \"4 GB\")"
  type        = string
  default     = "4 GB"
}

variable "max_concurrency" {
  description = "Max concurrent requests per instance"
  type        = number
  default     = 5
}

variable "min_instances" {
  description = "Minimum instances (App Runner minimum is 1)"
  type        = number
  default     = 1
}

variable "max_instances" {
  description = "Maximum instances"
  type        = number
  default     = 40
}

# --- Application secrets (values land in AWS Secrets Manager) ---

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
