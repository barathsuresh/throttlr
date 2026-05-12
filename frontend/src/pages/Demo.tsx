import { useEffect, useRef, useState } from 'react'
import { Activity, ArrowRight, Flame, TimerReset, Zap } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { AppShell } from '@/components/AppShell'
import { useBackendHealth } from '@/hooks/useBackendHealth'
import { createDemoAppKey } from '@/api/demo'
import { check } from '@/api/check'
import { API_BASE_URL } from '@/api/axios'
import type { DemoAppResponse, DemoRuleConfig, CheckResponse } from '@/types'
import axios from 'axios'

interface HistoryEntry {
  allowed: boolean
  remaining: number
  resetAfterMs: number
  timestamp: number
}

interface TabPanelProps {
  rule: DemoRuleConfig
  appKey: string
  expiresAt: number
}

const TAB_LABELS: Record<string, string> = {
  FIXED_WINDOW: 'Fixed Window',
  TOKEN_BUCKET: 'Token Bucket',
  SLIDING_WINDOW: 'Sliding Window',
}

const ALGORITHM_COPY: Record<string, string> = {
  FIXED_WINDOW: 'A hard counter for the current window. Simple, fast, and predictable.',
  TOKEN_BUCKET: 'Requests spend tokens; tokens refill over time. Great for bursty traffic.',
  SLIDING_WINDOW: 'Uses recent request timestamps to avoid sharp boundary resets.',
}

function AlgoTab({ rule, appKey, expiresAt }: TabPanelProps) {
  const [history, setHistory] = useState<HistoryEntry[]>([])
  const [last, setLast] = useState<CheckResponse | null>(null)
  const [pending, setPending] = useState(false)
  const [msLeft, setMsLeft] = useState(Number.POSITIVE_INFINITY)

  useEffect(() => {
    const id = setInterval(() => setMsLeft(expiresAt - Date.now()), 1000)
    return () => clearInterval(id)
  }, [expiresAt])

  const handleSend = async () => {
    setPending(true)
    try {
      const res = await check(appKey, { clientId: rule.clientId })
      setLast(res)
      setHistory((prev) => [{ ...res, timestamp: Date.now() }, ...prev].slice(0, 10))
    } finally {
      setPending(false)
    }
  }

  const expired = msLeft <= 0
  const remainingPercent = last
    ? Math.max(0, Math.min(100, (last.remaining / rule.limitPerWindow) * 100))
    : 100

  return (
    <div className="grid gap-5 lg:grid-cols-[0.9fr_1.1fr]">
      <div className="rounded-3xl border border-slate-900/10 bg-white/75 p-5 shadow-sm">
        <div className="mb-5 flex items-start justify-between gap-4">
          <div>
            <p className="font-mono text-xs uppercase tracking-[0.18em] text-slate-500">Selected scenario</p>
            <p className="mt-2 font-heading text-3xl font-bold tracking-tight">{TAB_LABELS[rule.algorithm] ?? rule.algorithm}</p>
          </div>
          <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-slate-950 text-amber-200">
            <Zap className="size-5" />
          </span>
        </div>
        <p className="mt-2 text-sm leading-6 text-slate-600">{ALGORITHM_COPY[rule.algorithm]}</p>

        <div className="mt-6 grid gap-3 font-mono text-sm">
          <div className="rounded-2xl border border-slate-900/10 bg-slate-950/5 p-3">
            <p className="mb-1 text-[0.65rem] uppercase tracking-[0.18em] text-slate-500">clientId</p>
            <span className="break-all text-slate-950">{rule.clientId}</span>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl border border-slate-900/10 bg-slate-950/5 p-3">
              <p className="mb-1 text-[0.65rem] uppercase tracking-[0.18em] text-slate-500">limit</p>
              <span className="text-slate-950">{rule.limitPerWindow}</span>
            </div>
            <div className="rounded-2xl border border-slate-900/10 bg-slate-950/5 p-3">
              <p className="mb-1 text-[0.65rem] uppercase tracking-[0.18em] text-slate-500">window</p>
              <span className="text-slate-950">{rule.windowMs}ms</span>
            </div>
          </div>
          <Badge variant={expired ? 'destructive' : 'outline'} className="w-fit rounded-full bg-white/70">
            {expired ? 'Demo expired' : Number.isFinite(msLeft) ? `Expires in ${Math.floor(msLeft / 1000)}s` : 'Preparing demo'}
          </Badge>
        </div>

        <Button className="mt-6 h-12 w-full rounded-2xl px-5 sm:w-auto" onClick={handleSend} disabled={pending || expired}>
          {pending ? 'Sending...' : 'Send Request'}
          <ArrowRight className="ml-1 size-4" />
        </Button>
      </div>

      <div className="ink-panel rounded-3xl p-5">
        <div className="flex items-center justify-between">
          <div>
            <p className="font-mono text-xs uppercase tracking-[0.18em] text-amber-200">Decision output</p>
            <p className="font-heading text-2xl font-bold">Runtime response</p>
          </div>
          <Activity className="size-7 text-cyan-200" />
        </div>

        {last ? (
          <div className="mt-5 space-y-4">
            <div className={`rounded-3xl border p-5 ${last.allowed ? 'border-emerald-300/30 bg-emerald-300/10' : 'border-red-300/30 bg-red-300/10'}`}>
              <div className="flex items-center justify-between gap-3">
                <Badge className={last.allowed ? 'bg-emerald-300 text-emerald-950' : 'bg-red-300 text-red-950'}>
                  {last.allowed ? 'ALLOWED' : 'BLOCKED'}
                </Badge>
                <span className="font-mono text-xs text-slate-400">{new Date().toLocaleTimeString()}</span>
              </div>
              <div className="mt-4 h-3 overflow-hidden rounded-full bg-white/10">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-amber-200 to-emerald-300 transition-all"
                  style={{ width: `${remainingPercent}%` }}
                />
              </div>
              <p className="mt-4 font-mono text-sm text-slate-300">
                remaining={last.remaining} retryAfterMs={last.retryAfterMs} resetAfterMs={last.resetAfterMs}
              </p>
            </div>

            {history.length > 0 && (
              <div className="space-y-2">
                <p className="font-mono text-xs uppercase tracking-[0.18em] text-slate-400">Last 10 checks</p>
                {history.map((h, i) => (
                  <div key={`${h.timestamp}-${i}`} className="flex items-center gap-3 rounded-2xl bg-white/5 px-3 py-2 text-xs text-slate-300">
                    <span>{new Date(h.timestamp).toLocaleTimeString()}</span>
                    <Badge variant="outline" className={h.allowed ? 'border-emerald-300/40 text-emerald-200' : 'border-red-300/40 text-red-200'}>
                      {h.allowed ? 'allowed' : 'blocked'}
                    </Badge>
                    <span className="ml-auto font-mono">remaining={h.remaining}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className="mt-5 grid min-h-64 place-items-center rounded-3xl border border-dashed border-white/15 p-8 text-center text-slate-400">
            <div>
            <Flame className="mx-auto mb-4 size-8 text-amber-200" />
            Press “Send Request” to watch Redis decide.
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export function Demo() {
  const { isAlive } = useBackendHealth()
  const [demo, setDemo] = useState<DemoAppResponse | null>(null)
  const [expiresAt, setExpiresAt] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selectedAlgorithm, setSelectedAlgorithm] = useState<string | null>(null)
  const fetched = useRef(false)

  useEffect(() => {
    if (!isAlive || fetched.current) return
    fetched.current = true
    createDemoAppKey()
      .then((d) => {
        setDemo(d)
        setExpiresAt(Date.now() + d.expiresInMs)
        setSelectedAlgorithm(d.rules[0]?.algorithm ?? null)
      })
      .catch((err) => {
        if (axios.isAxiosError(err) && !err.response) {
          setError(`Cannot reach the backend at ${API_BASE_URL}. Start the backend or set VITE_API_URL to your deployed API.`)
          return
        }
        if (axios.isAxiosError(err)) {
          const status = err.response?.status
          const message = err.response?.data?.message
          setError(`Demo session request failed${status ? ` with HTTP ${status}` : ''}${message ? `: ${message}` : '.'}`)
          return
        }
        setError('Failed to create demo session. Try refreshing.')
      })
  }, [isAlive])

  const selectedRule = demo?.rules.find((rule) => rule.algorithm === selectedAlgorithm) ?? demo?.rules[0]

  return (
    <AppShell
      eyebrow="Live Playground"
      title="Watch the limiter make real decisions."
      description="Each tab creates a temporary demo rule and calls the same /api/check endpoint your API would use in production."
    >
      {!isAlive && (
        <div className="glass-panel rounded-3xl p-6 text-sm text-slate-600">
          Backend is waking up on Cloud Run. Give it a few seconds.
        </div>
      )}

      {error && (
        <div className="glass-panel rounded-3xl border-red-200 p-6 text-sm text-red-700">{error}</div>
      )}

      {demo && expiresAt !== null && (
        <div className="rounded-[2rem] border border-slate-900/10 bg-white/70 p-4 shadow-sm backdrop-blur md:p-5">
          <div className="mb-5 grid gap-4 lg:grid-cols-[1fr_auto] lg:items-center">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-3">
                <Badge className="bg-slate-950 text-amber-200">Temporary key</Badge>
                <span className="break-all font-mono text-xs text-slate-500">{demo.appId}</span>
              </div>
              <p className="mt-2 text-sm text-slate-600">
                Choose an algorithm, send requests, and watch the decision state change.
              </p>
            </div>
            <span className="flex items-center gap-2 rounded-full border border-slate-900/10 bg-white/70 px-3 py-2 text-sm text-slate-600">
              <TimerReset className="size-4" />
              Auto-expires
            </span>
          </div>

          <div className="mb-5 grid gap-2 rounded-3xl bg-slate-950 p-2 sm:grid-cols-3" role="tablist" aria-label="Demo algorithms">
              {demo.rules.map((rule) => (
                <button
                  key={rule.algorithm}
                  type="button"
                  role="tab"
                  aria-selected={selectedAlgorithm === rule.algorithm}
                  className={`rounded-2xl px-4 py-3 text-left transition ${
                    selectedAlgorithm === rule.algorithm
                      ? 'bg-amber-200 text-slate-950 shadow-lg'
                      : 'text-slate-300 hover:bg-white/10 hover:text-white'
                  }`}
                  onClick={() => setSelectedAlgorithm(rule.algorithm)}
                >
                  <span className="block font-heading text-base font-bold">{TAB_LABELS[rule.algorithm] ?? rule.algorithm}</span>
                  <span className="mt-1 block font-mono text-[0.65rem] uppercase tracking-[0.14em] opacity-70">
                    {rule.limitPerWindow}/{Math.round(rule.windowMs / 1000)}s
                  </span>
                </button>
              ))}
          </div>

          {selectedRule && (
            <AlgoTab rule={selectedRule} appKey={demo.appKey} expiresAt={expiresAt} />
          )}
        </div>
      )}
    </AppShell>
  )
}
