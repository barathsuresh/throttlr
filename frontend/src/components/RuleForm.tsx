import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Algorithm, CreateRuleRequest, RuleResponse } from '@/types'

interface Props {
  onSubmit: (body: CreateRuleRequest) => Promise<void>
  onCancel: () => void
  pending: boolean
  initial?: RuleResponse
}

const ALGORITHMS: Algorithm[] = ['FIXED_WINDOW', 'TOKEN_BUCKET', 'SLIDING_WINDOW']

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
    <form onSubmit={handleSubmit} className="space-y-3 border rounded-md p-4 bg-white">
      <div className="grid grid-cols-2 gap-3">
        <div className="col-span-2 space-y-1">
          <Label>Client ID</Label>
          <Input
            placeholder="e.g. user:123 or ip:1.2.3.4"
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            disabled={!!initial}
          />
        </div>
        <div className="space-y-1">
          <Label>Algorithm</Label>
          <select
            className="w-full rounded-md border px-3 py-2 text-sm bg-white"
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
            type="number"
            min={1}
            placeholder="e.g. 60000 for 1 minute"
            value={windowMs}
            onChange={(e) => setWindowMs(e.target.value)}
          />
        </div>
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <div className="flex gap-2">
        <Button type="submit" size="sm" disabled={pending}>
          {pending ? 'Saving…' : 'Save rule'}
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}
