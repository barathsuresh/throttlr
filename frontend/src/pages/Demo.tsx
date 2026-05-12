import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { BackendHealthBanner } from '@/components/BackendHealthBanner'
import { useBackendHealth } from '@/hooks/useBackendHealth'
import { createDemoAppKey } from '@/api/demo'
import { check } from '@/api/check'
import type { DemoAppResponse, DemoRuleConfig, CheckResponse } from '@/types'

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

function AlgoTab({ rule, appKey, expiresAt }: TabPanelProps) {
  const [history, setHistory] = useState<HistoryEntry[]>([])
  const [last, setLast] = useState<CheckResponse | null>(null)
  const [pending, setPending] = useState(false)
  const [msLeft, setMsLeft] = useState(expiresAt - Date.now())

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
  const expiryLabel = expired
    ? 'Demo expired'
    : `Expires in ${Math.floor(msLeft / 1000)}s`

  return (
    <div className="space-y-4 pt-4">
      <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
        <span>Client: <code className="text-foreground">{rule.clientId}</code></span>
        <span>Limit: <code className="text-foreground">{rule.limitPerWindow} / {rule.windowMs}ms</code></span>
        <Badge variant={expired ? 'destructive' : 'outline'}>{expiryLabel}</Badge>
      </div>

      <Button onClick={handleSend} disabled={pending || expired}>
        {pending ? 'Sending…' : 'Send Request'}
      </Button>

      {last && (
        <div className={`rounded-md border p-4 text-sm space-y-1 ${last.allowed ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50'}`}>
          <div className="flex gap-2 items-center">
            <Badge className={last.allowed ? 'bg-green-600' : 'bg-red-600'}>
              {last.allowed ? 'ALLOWED' : 'BLOCKED'}
            </Badge>
            <span>Remaining: <strong>{last.remaining}</strong></span>
          </div>
          {!last.allowed && (
            <p className="text-muted-foreground">Retry after: {last.retryAfterMs}ms</p>
          )}
          <p className="text-muted-foreground">Reset in: {last.resetAfterMs}ms</p>
        </div>
      )}

      {history.length > 0 && (
        <div className="space-y-1">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">History (last 10)</p>
          {history.map((h, i) => (
            <div key={i} className="flex gap-2 text-xs text-muted-foreground">
              <span>{new Date(h.timestamp).toLocaleTimeString()}</span>
              <Badge variant="outline" className={h.allowed ? 'text-green-700 border-green-300' : 'text-red-700 border-red-300'}>
                {h.allowed ? 'allowed' : 'blocked'}
              </Badge>
              <span>remaining={h.remaining}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

const TAB_LABELS: Record<string, string> = {
  FIXED_WINDOW: 'Fixed Window',
  TOKEN_BUCKET: 'Token Bucket',
  SLIDING_WINDOW: 'Sliding Window',
}

export function Demo() {
  const { isAlive } = useBackendHealth()
  const [demo, setDemo] = useState<DemoAppResponse | null>(null)
  const [expiresAt, setExpiresAt] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const fetched = useRef(false)

  useEffect(() => {
    if (!isAlive || fetched.current) return
    fetched.current = true
    createDemoAppKey()
      .then((d) => {
        setDemo(d)
        setExpiresAt(Date.now() + d.expiresInMs)
      })
      .catch(() => setError('Failed to create demo session. Try refreshing.'))
  }, [isAlive])

  return (
    <div className="min-h-screen bg-slate-50">
      <BackendHealthBanner isAlive={isAlive} />
      <header className="border-b bg-white px-6 py-4 flex items-center gap-4">
        <Link to="/" className="text-sm text-muted-foreground hover:underline">← Home</Link>
        <span className="font-semibold">Live Demo</span>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-xl font-semibold mb-2">Rate Limiter Playground</h1>
        <p className="text-sm text-muted-foreground mb-6">
          Each tab runs a separate rate-limit counter. Hit "Send Request" to see allowed/blocked decisions in real time.
        </p>

        {!isAlive && (
          <p className="text-sm text-muted-foreground">Waiting for backend…</p>
        )}

        {error && (
          <p className="text-sm text-red-600">{error}</p>
        )}

        {demo && expiresAt !== null && (
          <Tabs defaultValue={demo.rules[0].algorithm}>
            <TabsList>
              {demo.rules.map((rule) => (
                <TabsTrigger key={rule.algorithm} value={rule.algorithm}>
                  {TAB_LABELS[rule.algorithm] ?? rule.algorithm}
                </TabsTrigger>
              ))}
            </TabsList>
            {demo.rules.map((rule) => (
              <TabsContent key={rule.algorithm} value={rule.algorithm}>
                <AlgoTab
                  rule={rule}
                  appKey={demo.appKey}
                  expiresAt={expiresAt}
                />
              </TabsContent>
            ))}
          </Tabs>
        )}
      </main>
    </div>
  )
}
