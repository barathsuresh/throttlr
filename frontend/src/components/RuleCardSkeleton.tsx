export function RuleCardSkeleton() {
  return (
    <div className="glass-panel animate-pulse rounded-3xl p-5">
      <div className="flex items-start justify-between">
        <div>
          <div className="mb-3 h-10 w-10 rounded-2xl bg-slate-200 dark:bg-slate-700" />
          <div className="h-4 w-36 rounded bg-slate-200 dark:bg-slate-700" />
        </div>
        <div className="h-6 w-24 rounded-full bg-slate-100 dark:bg-slate-700/60" />
      </div>
      <div className="mt-4 h-3 w-32 rounded bg-slate-100 dark:bg-slate-700/60" />
      <div className="mt-4 flex gap-2">
        <div className="h-8 w-14 rounded-xl bg-slate-200 dark:bg-slate-700" />
        <div className="h-8 w-16 rounded-xl bg-slate-100 dark:bg-slate-700/60" />
      </div>
    </div>
  )
}
