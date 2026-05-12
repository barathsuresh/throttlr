import { useQuery } from '@tanstack/react-query'
import { getAnalytics } from '@/api/analytics'

export function useAnalytics(appId: string, enabled = true) {
  return useQuery({
    queryKey: ['analytics', appId],
    queryFn: () => getAnalytics(appId),
    enabled,
  })
}
