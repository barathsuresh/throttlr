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
    <div className="grid gap-3 rounded-2xl border border-slate-900/10 bg-white/60 p-3 text-sm sm:grid-cols-3">
      <div>
        <p className="font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-500">Total</p>
        <Badge variant="outline" className="mt-2 bg-white">{data.total}</Badge>
      </div>
      <div>
        <p className="font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-500">Allowed</p>
        <Badge variant="outline" className="mt-2 border-emerald-300 bg-emerald-50 text-emerald-800">
          {data.allowed}
        </Badge>
      </div>
      <div>
        <p className="font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-500">Blocked</p>
        <Badge variant="outline" className="mt-2 border-red-300 bg-red-50 text-red-800">
          {data.blocked}
        </Badge>
      </div>
    </div>
  )
}
