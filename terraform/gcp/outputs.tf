output "service_url" {
  description = "Public URL of the Cloud Run service"
  value       = google_cloud_run_v2_service.throttlr.uri
}

output "artifact_registry_repository" {
  description = "Docker image path prefix for pushes"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.throttlr.repository_id}"
}

output "runtime_service_account" {
  description = "Service account the Cloud Run service runs as"
  value       = google_service_account.runtime.email
}
