export function AppCardSkeleton() {
  return (
    <div className="glass-panel animate-pulse rounded-3xl border-white/60 bg-white/65 p-6">
      <div className="mb-3 h-10 w-10 rounded-2xl bg-slate-200 dark:bg-slate-700" />
      <div className="mb-2 h-6 w-40 rounded-lg bg-slate-200 dark:bg-slate-700" />
      <div className="mb-6 h-3 w-56 rounded bg-slate-100 dark:bg-slate-700/60" />
      <div className="flex gap-2">
        <div className="h-8 w-24 rounded-xl bg-slate-200 dark:bg-slate-700" />
        <div className="h-8 w-32 rounded-xl bg-slate-100 dark:bg-slate-700/60" />
        <div className="h-8 w-16 rounded-xl bg-slate-100 dark:bg-slate-700/60" />
      </div>
    </div>
  )
}
