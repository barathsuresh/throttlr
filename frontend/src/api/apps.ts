import api from './axios'
import type { CreateAppRequest, AppCreatedResponse, AppResponse, PagedResponse } from '@/types'

export const createApp = (body: CreateAppRequest): Promise<AppCreatedResponse> =>
  api.post<AppCreatedResponse>('/api/apps', body).then((r) => r.data)

export const listApps = (page: number, size = 10): Promise<PagedResponse<AppResponse>> =>
  api.get<PagedResponse<AppResponse>>('/api/apps', { params: { page, size } }).then((r) => r.data)

export const deleteApp = (appId: string): Promise<void> =>
  api.delete(`/api/apps/${appId}`).then(() => undefined)
