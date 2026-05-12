interface Props {
  isAlive: boolean
}

export function BackendHealthBanner({ isAlive }: Props) {
  if (isAlive) return null

  return (
    <div className="w-full border-b border-amber-300/40 bg-amber-100/80 px-4 py-2 text-sm text-amber-950 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center gap-2">
      <svg className="animate-spin h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
      <span>Backend is waking up on Cloud Run — this takes a few seconds after idle…</span>
      </div>
    </div>
  )
}
