export type Algorithm = 'FIXED_WINDOW' | 'TOKEN_BUCKET' | 'SLIDING_WINDOW'

export interface PagedResponse<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface RegisterResponse {
  accountId: string
  passphrase: string
  message: string
}

export interface LoginRequest {
  passphrase: string
}

export interface LoginResponse {
  sessionToken: string
  accountId: string
}

export interface MeResponse {
  accountId: string
}

export interface CreateAppRequest {
  name: string
}

export interface AppCreatedResponse {
  appId: string
  name: string
  appKey: string
  message: string
}

export interface AppResponse {
  appId: string
  name: string
  ruleCount: number
  createdAt: number
}

export interface CreateRuleRequest {
  clientId: string
  algorithm: Algorithm
  limitPerWindow: number
  windowMs: number
}

export interface RuleResponse {
  ruleId: string
  clientId: string
  algorithm: Algorithm
  limitPerWindow: number
  windowMs: number
}

export interface AnalyticsResponse {
  appId: string
  hour: number
  total: number
  allowed: number
  blocked: number
}

export interface CheckRequest {
  clientId: string
}

export interface CheckResponse {
  allowed: boolean
  remaining: number
  resetAfterMs: number
  retryAfterMs: number
}

export interface DemoRuleConfig {
  clientId: string
  algorithm: string
  limitPerWindow: number
  windowMs: number
}

export interface DemoAppResponse {
  appId: string
  appKey: string
  expiresInMs: number
  rules: DemoRuleConfig[]
  message: string
}

export interface HealthResponse {
  status: string
  component: string
}

export interface ErrorResponse {
  message: string
  status: number
  timestamp: number
}
