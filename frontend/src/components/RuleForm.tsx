import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import type { Algorithm, CreateRuleRequest, RuleResponse } from '@/types'

interface Props {
  onSubmit: (body: CreateRuleRequest) => Promise<void>
  onCancel: () => void
  pending: boolean
  initial?: RuleResponse
}

const ALGORITHMS: Algorithm[] = ['FIXED_WINDOW', 'TOKEN_BUCKET', 'SLIDING_WINDOW']
const PATTERN_EXAMPLES = ['user:123', 'user:*', 'ip:10.0.*', '*']

export function RuleForm({ onSubmit, onCancel, pending, initial }: Props) {
  const [clientId, setClientId] = useState(initial?.clientId ?? '')
  const [algorithm, setAlgorithm] = useState<Algorithm>(initial?.algorithm ?? 'FIXED_WINDOW')
  const [limitPerWindow, setLimitPerWindow] = useState(String(initial?.limitPerWindow ?? ''))
  const [windowMs, setWindowMs] = useState(String(initial?.windowMs ?? ''))
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    const limit = parseInt(limitPerWindow, 10)
    const window = parseInt(windowMs, 10)
    if (!clientId.trim() || isNaN(limit) || limit < 1 || isNaN(window) || window < 1) {
      setError('All fields required. Limit and window must be ≥ 1.')
      return
    }
    try {
      await onSubmit({ clientId: clientId.trim(), algorithm, limitPerWindow: limit, windowMs: window })
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? 'Failed to save rule')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="glass-panel space-y-4 rounded-3xl p-5">
      <div>
        <p className="font-heading text-xl font-bold">{initial ? 'Edit rule' : 'Create rule'}</p>
        <p className="mt-1 text-sm text-slate-600">
          Use an exact clientId or a wildcard pattern. Pattern matches still get isolated counters per real client.
        </p>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="col-span-2 space-y-1">
          <Label>Client ID</Label>
          <Input
            className="h-10 rounded-2xl bg-white/70"
            placeholder="e.g. user:123, user:*, ip:10.0.*, or *"
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            disabled={!!initial}
          />
          <div className="flex flex-wrap gap-2 pt-2">
            {PATTERN_EXAMPLES.map((example) => (
              <Badge
                key={example}
                variant="outline"
                className="cursor-pointer rounded-full bg-white/70 font-mono text-xs"
                onClick={() => !initial && setClientId(example)}
              >
                {example}
              </Badge>
            ))}
          </div>
        </div>
        <div className="space-y-1">
          <Label>Algorithm</Label>
          <select
            className="h-10 w-full rounded-2xl border border-input bg-white/70 px-3 py-2 text-sm outline-none transition focus:border-ring focus:ring-3 focus:ring-ring/40"
            value={algorithm}
            onChange={(e) => setAlgorithm(e.target.value as Algorithm)}
          >
            {ALGORITHMS.map((a) => (
              <option key={a} value={a}>
                {a.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1">
          <Label>Limit per window</Label>
          <Input
            className="h-10 rounded-2xl bg-white/70"
            type="number"
            min={1}
            placeholder="e.g. 100"
            value={limitPerWindow}
            onChange={(e) => setLimitPerWindow(e.target.value)}
          />
        </div>
        <div className="col-span-2 space-y-1">
          <Label>Window (ms)</Label>
          <Input
            className="h-10 rounded-2xl bg-white/70"
            type="number"
            min={1}
            placeholder="e.g. 60000 for 1 minute"
            value={windowMs}
            onChange={(e) => setWindowMs(e.target.value)}
          />
        </div>
      </div>
      {error && <p className="text-sm text-red-700">{error}</p>}
      <div className="flex gap-2">
        <Button type="submit" size="sm" className="rounded-xl" disabled={pending}>
          {pending ? 'Saving...' : 'Save rule'}
        </Button>
        <Button type="button" size="sm" variant="ghost" className="rounded-xl" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}
