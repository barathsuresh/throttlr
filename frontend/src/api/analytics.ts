import api from './axios'
import type { AnalyticsResponse } from '@/types'

export const getAnalytics = (appId: string): Promise<AnalyticsResponse> =>
  api.get<AnalyticsResponse>(`/api/apps/${appId}/analytics`).then((r) => r.data)
