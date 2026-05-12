import api from './axios'
import type { RegisterResponse, LoginRequest, LoginResponse, MeResponse } from '@/types'

export const register = (): Promise<RegisterResponse> =>
  api.post<RegisterResponse>('/api/auth/register').then((r) => r.data)

export const login = (body: LoginRequest): Promise<LoginResponse> =>
  api.post<LoginResponse>('/api/auth/login', body).then((r) => r.data)

export const me = (): Promise<MeResponse> =>
  api.get<MeResponse>('/api/auth/me').then((r) => r.data)
