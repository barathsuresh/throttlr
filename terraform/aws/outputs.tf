output "service_url" {
  description = "Public URL of the App Runner service"
  value       = "https://${aws_apprunner_service.throttlr.service_url}"
}

output "ecr_repository_url" {
  description = "ECR repository URL for image pushes"
  value       = aws_ecr_repository.throttlr.repository_url
}

output "apprunner_service_arn" {
  description = "App Runner service ARN"
  value       = aws_apprunner_service.throttlr.arn
}
