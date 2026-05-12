import api from './axios'
import type { DemoAppResponse } from '@/types'

export const createDemoAppKey = (): Promise<DemoAppResponse> =>
  api.post<DemoAppResponse>('/api/demo/app-key').then((r) => r.data)
