import api from './axios'
import type { CheckRequest, CheckResponse } from '@/types'

export const check = (appKey: string, body: CheckRequest): Promise<CheckResponse> =>
  api
    .post<CheckResponse>('/api/check', body, { headers: { 'X-App-Key': appKey } })
    .then((r) => r.data)
