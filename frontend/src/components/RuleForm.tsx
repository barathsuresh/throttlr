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

type WindowUnit = 'ms' | 'seconds' | 'minutes' | 'hours'

const UNIT_MULTIPLIERS: Record<WindowUnit, number> = {
  ms: 1,
  seconds: 1_000,
  minutes: 60_000,
  hours: 3_600_000,
}

function msToUnitValue(ms: number): { value: number; unit: WindowUnit } {
  if (ms % 3_600_000 === 0) return { value: ms / 3_600_000, unit: 'hours' }
  if (ms % 60_000 === 0) return { value: ms / 60_000, unit: 'minutes' }
  if (ms % 1_000 === 0) return { value: ms / 1_000, unit: 'seconds' }
  return { value: ms, unit: 'ms' }
}

const ALGORITHMS: Algorithm[] = ['FIXED_WINDOW', 'TOKEN_BUCKET', 'SLIDING_WINDOW']
const PATTERN_EXAMPLES = ['user:123', 'user:*', 'ip:10.0.*', '*']

export function RuleForm({ onSubmit, onCancel, pending, initial }: Props) {
  const [clientId, setClientId] = useState(initial?.clientId ?? '')
  const [algorithm, setAlgorithm] = useState<Algorithm>(initial?.algorithm ?? 'FIXED_WINDOW')
  const [limitPerWindow, setLimitPerWindow] = useState(String(initial?.limitPerWindow ?? ''))

  const initialWindow = initial?.windowMs ? msToUnitValue(initial.windowMs) : { value: '', unit: 'minutes' as WindowUnit }
  const [windowValue, setWindowValue] = useState(String(initialWindow.value))
  const [windowUnit, setWindowUnit] = useState<WindowUnit>(initialWindow.unit)

  const [error, setError] = useState<string | null>(null)

  const computedWindowMs = Math.round(parseFloat(windowValue) * UNIT_MULTIPLIERS[windowUnit])

  const windowPreview = (() => {
    const val = parseFloat(windowValue)
    if (isNaN(val) || val <= 0) return null
    const ms = val * UNIT_MULTIPLIERS[windowUnit]
    if (windowUnit !== 'ms') return null
    if (ms % 3_600_000 === 0) return `= ${ms / 3_600_000} hour${ms / 3_600_000 !== 1 ? 's' : ''}`
    if (ms % 60_000 === 0) return `= ${ms / 60_000} minute${ms / 60_000 !== 1 ? 's' : ''}`
    if (ms % 1_000 === 0) return `= ${ms / 1_000} second${ms / 1_000 !== 1 ? 's' : ''}`
    return null
  })()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    const limit = parseInt(limitPerWindow, 10)
    const windowMs = computedWindowMs
    if (!clientId.trim() || isNaN(limit) || limit < 1 || isNaN(windowMs) || windowMs < 1) {
      setError('All fields required. Limit and window must be ≥ 1.')
      return
    }
    try {
      await onSubmit({ clientId: clientId.trim(), algorithm, limitPerWindow: limit, windowMs })
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? 'Failed to save rule')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="glass-panel space-y-4 rounded-3xl p-5">
      <div>
        <p className="font-heading text-xl font-bold dark:text-slate-50">{initial ? 'Edit rule' : 'Create rule'}</p>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          Use an exact clientId or a wildcard pattern. Pattern matches still get isolated counters per real client.
        </p>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="col-span-2 space-y-1">
          <Label>Client ID</Label>
          <Input
            className="h-10 rounded-2xl bg-white/70 dark:bg-slate-800/70 dark:text-slate-100 dark:placeholder:text-slate-500"
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
                className="cursor-pointer rounded-full bg-white/70 font-mono text-xs dark:bg-slate-800/70 dark:text-slate-300 dark:border-white/10"
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
            className="h-10 w-full rounded-2xl border border-input bg-white/70 px-3 py-2 text-sm outline-none transition focus:border-ring focus:ring-3 focus:ring-ring/40 dark:bg-slate-800/70 dark:text-slate-100 dark:border-white/10"
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
            className="h-10 rounded-2xl bg-white/70 dark:bg-slate-800/70 dark:text-slate-100 dark:placeholder:text-slate-500"
            type="number"
            min={1}
            placeholder="e.g. 100"
            value={limitPerWindow}
            onChange={(e) => setLimitPerWindow(e.target.value)}
          />
        </div>
        <div className="col-span-2 space-y-1">
          <Label>Window</Label>
          <div className="flex gap-2">
            <Input
              className="h-10 rounded-2xl bg-white/70 dark:bg-slate-800/70 dark:text-slate-100 dark:placeholder:text-slate-500"
              type="number"
              min={1}
              placeholder="e.g. 1"
              value={windowValue}
              onChange={(e) => setWindowValue(e.target.value)}
            />
            <select
              className="h-10 rounded-2xl border border-input bg-white/70 px-3 py-2 text-sm outline-none transition focus:border-ring focus:ring-3 focus:ring-ring/40"
              value={windowUnit}
              onChange={(e) => setWindowUnit(e.target.value as WindowUnit)}
            >
              <option value="ms">ms</option>
              <option value="seconds">seconds</option>
              <option value="minutes">minutes</option>
              <option value="hours">hours</option>
            </select>
          </div>
          {windowPreview && (
            <p className="text-xs text-slate-500 dark:text-slate-400">{windowPreview}</p>
          )}
          {windowValue && !isNaN(parseFloat(windowValue)) && parseFloat(windowValue) > 0 && windowUnit !== 'ms' && (
            <p className="text-xs text-slate-400 dark:text-slate-500">= {computedWindowMs.toLocaleString()} ms</p>
          )}
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
