interface Props {
  isAlive: boolean
}

export function BackendHealthBanner({ isAlive }: Props) {
  if (isAlive) return null

  return (
    <div className="w-full bg-amber-50 border-b border-amber-200 px-4 py-2 flex items-center gap-2 text-sm text-amber-800">
      <svg className="animate-spin h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
      <span>Backend is waking up on Cloud Run — this takes a few seconds after idle…</span>
    </div>
  )
}
