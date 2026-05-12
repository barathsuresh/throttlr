import { useQuery } from '@tanstack/react-query'
import api from '@/api/axios'
import type { HealthResponse } from '@/types'

const fetchHealth = (): Promise<HealthResponse> =>
  api.get<HealthResponse>('/api/health').then((r) => r.data)

export function useBackendHealth() {
  const query = useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
    retry: false,
    refetchInterval: (query) =>
      query.state.status === 'success' ? false : 3000,
  })

  return { isAlive: query.isSuccess }
}
