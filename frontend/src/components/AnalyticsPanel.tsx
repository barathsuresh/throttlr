import { useAnalytics } from '@/hooks/useAnalytics'

interface Props {
  appId: string
}

export function AnalyticsPanel({ appId }: Props) {
  const { data, isLoading } = useAnalytics(appId)

  if (isLoading) {
    return (
      <div className="animate-pulse rounded-2xl border border-slate-900/10 bg-white/60 p-4 dark:border-white/10 dark:bg-slate-800/60">
        <div className="mb-3 h-3 w-28 rounded bg-slate-200 dark:bg-slate-700" />
        <div className="h-4 rounded-full bg-slate-200 dark:bg-slate-700" />
        <div className="mt-3 flex justify-between">
          <div className="h-3 w-16 rounded bg-slate-100 dark:bg-slate-700/60" />
          <div className="h-3 w-16 rounded bg-slate-100 dark:bg-slate-700/60" />
        </div>
      </div>
    )
  }

  if (!data) return null

  const allowedPct = data.total > 0 ? (data.allowed / data.total) * 100 : 0
  const blockedPct = data.total > 0 ? (data.blocked / data.total) * 100 : 0

  return (
    <div className="rounded-2xl border border-slate-900/10 bg-white/60 p-4 text-sm dark:border-white/10 dark:bg-slate-800/60">
      <div className="mb-3 flex items-center justify-between">
        <p className="font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">
          Current hour — {data.total.toLocaleString()} total
        </p>
      </div>

      {data.total === 0 ? (
        <p className="text-xs text-slate-400 dark:text-slate-500">No requests this hour.</p>
      ) : (
        <>
          <div className="flex h-3 w-full overflow-hidden rounded-full bg-slate-100 dark:bg-slate-700">
            <div
              className="h-full bg-emerald-400 transition-all duration-500"
              style={{ width: `${allowedPct}%` }}
            />
            <div
              className="h-full bg-red-400 transition-all duration-500"
              style={{ width: `${blockedPct}%` }}
            />
          </div>
          <div className="mt-3 flex gap-4">
            <div className="flex items-center gap-1.5">
              <span className="inline-block h-2 w-2 rounded-full bg-emerald-400" />
              <span className="text-xs text-slate-600 dark:text-slate-300">
                {data.allowed.toLocaleString()} allowed
                <span className="ml-1 text-slate-400 dark:text-slate-500">({allowedPct.toFixed(0)}%)</span>
              </span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="inline-block h-2 w-2 rounded-full bg-red-400" />
              <span className="text-xs text-slate-600 dark:text-slate-300">
                {data.blocked.toLocaleString()} blocked
                <span className="ml-1 text-slate-400">({blockedPct.toFixed(0)}%)</span>
              </span>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
