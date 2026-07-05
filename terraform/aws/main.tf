# --- ECR ---

resource "aws_ecr_repository" "throttlr" {
  name                 = var.service_name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "keep_recent" {
  repository = aws_ecr_repository.throttlr.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}

# --- Secrets Manager ---

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

resource "aws_secretsmanager_secret" "app" {
  for_each = local.secrets

  name = "${var.service_name}/${each.key}"
}

resource "aws_secretsmanager_secret_version" "app" {
  for_each = local.secrets

  secret_id     = aws_secretsmanager_secret.app[each.key].id
  secret_string = each.value
}

# --- IAM: App Runner pulls from ECR ---

resource "aws_iam_role" "apprunner_access" {
  name = "${var.service_name}-apprunner-access"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "build.apprunner.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "apprunner_ecr" {
  role       = aws_iam_role.apprunner_access.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess"
}

# --- IAM: runtime instance reads secrets ---

resource "aws_iam_role" "apprunner_instance" {
  name = "${var.service_name}-apprunner-instance"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "tasks.apprunner.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "instance_secrets" {
  name = "${var.service_name}-read-secrets"
  role = aws_iam_role.apprunner_instance.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = [for s in aws_secretsmanager_secret.app : s.arn]
    }]
  })
}

# --- App Runner ---

resource "aws_apprunner_auto_scaling_configuration_version" "throttlr" {
  auto_scaling_configuration_name = var.service_name

  max_concurrency = var.max_concurrency
  min_size        = var.min_instances
  max_size        = var.max_instances
}

resource "aws_apprunner_service" "throttlr" {
  service_name = var.service_name

  auto_scaling_configuration_arn = aws_apprunner_auto_scaling_configuration_version.throttlr.arn

  source_configuration {
    auto_deployments_enabled = false

    authentication_configuration {
      access_role_arn = aws_iam_role.apprunner_access.arn
    }

    image_repository {
      image_identifier      = "${aws_ecr_repository.throttlr.repository_url}:${var.image_tag}"
      image_repository_type = "ECR"

      image_configuration {
        port = "8080"

        runtime_environment_variables = {
          SPRING_PROFILES_ACTIVE  = "prod"
          TRUST_FORWARDED_HEADERS = "true"
          REDIS_SSL_ENABLED       = "true"
        }

        runtime_environment_secrets = {
          for name, secret in aws_secretsmanager_secret.app : name => secret.arn
        }
      }
    }
  }

  instance_configuration {
    cpu               = var.cpu
    memory            = var.memory
    instance_role_arn = aws_iam_role.apprunner_instance.arn
  }

  health_check_configuration {
    protocol = "HTTP"
    path     = "/api/health"
    interval = 10
    timeout  = 5
  }

  depends_on = [
    aws_secretsmanager_secret_version.app,
    aws_iam_role_policy.instance_secrets,
    aws_iam_role_policy_attachment.apprunner_ecr,
  ]
}
