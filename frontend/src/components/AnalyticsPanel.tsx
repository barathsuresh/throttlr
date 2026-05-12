import { useAnalytics } from '@/hooks/useAnalytics'
import { Badge } from '@/components/ui/badge'

interface Props {
  appId: string
}

export function AnalyticsPanel({ appId }: Props) {
  const { data, isLoading } = useAnalytics(appId)

  if (isLoading) return <p className="text-xs text-muted-foreground px-1">Loading analytics…</p>
  if (!data) return null

  return (
    <div className="flex gap-3 text-sm py-2">
      <span className="text-muted-foreground">Current hour:</span>
      <Badge variant="outline">Total {data.total}</Badge>
      <Badge variant="outline" className="text-green-700 border-green-300">
        Allowed {data.allowed}
      </Badge>
      <Badge variant="outline" className="text-red-700 border-red-300">
        Blocked {data.blocked}
      </Badge>
    </div>
  )
}
