import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { RuleForm } from '@/components/RuleForm'
import { useRules } from '@/hooks/useRules'
import { toast } from 'sonner'
import type { CreateRuleRequest, RuleResponse } from '@/types'

const ALGORITHM_COLORS: Record<string, string> = {
  FIXED_WINDOW: 'bg-blue-100 text-blue-800',
  TOKEN_BUCKET: 'bg-purple-100 text-purple-800',
  SLIDING_WINDOW: 'bg-green-100 text-green-800',
}

export function AppRules() {
  const { appId } = useParams<{ appId: string }>()
  const [page] = useState(0)
  const { rules, isLoading, createRule, createPending, updateRule, updatePending, deleteRule } =
    useRules(appId!, page)

  const [adding, setAdding] = useState(false)
  const [editing, setEditing] = useState<RuleResponse | null>(null)

  const handleCreate = async (body: CreateRuleRequest) => {
    await createRule(body)
    setAdding(false)
    toast.success('Rule created')
  }

  const handleUpdate = async (body: CreateRuleRequest) => {
    await updateRule({ clientId: editing!.clientId, body })
    setEditing(null)
    toast.success('Rule updated')
  }

  const handleDelete = async (clientId: string) => {
    try {
      await deleteRule(clientId)
      toast.success('Rule deleted')
    } catch {
      toast.error('Failed to delete rule')
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b bg-white px-6 py-4 flex items-center gap-4">
        <Link to="/dashboard" className="text-sm text-muted-foreground hover:underline">
          ← Dashboard
        </Link>
        <span className="font-semibold">Rules</span>
        <span className="text-xs text-muted-foreground font-mono">{appId}</span>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-8 space-y-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-semibold">Rules</h1>
          {!adding && !editing && (
            <Button size="sm" onClick={() => setAdding(true)}>
              Add Rule
            </Button>
          )}
        </div>

        {adding && (
          <RuleForm
            onSubmit={handleCreate}
            onCancel={() => setAdding(false)}
            pending={createPending}
          />
        )}

        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}

        {rules?.items.map((rule) =>
          editing?.ruleId === rule.ruleId ? (
            <RuleForm
              key={rule.ruleId}
              onSubmit={handleUpdate}
              onCancel={() => setEditing(null)}
              pending={updatePending}
              initial={rule}
            />
          ) : (
            <div key={rule.ruleId} className="border rounded-md p-4 bg-white space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-sm">{rule.clientId}</span>
                <Badge className={ALGORITHM_COLORS[rule.algorithm] ?? ''} variant="outline">
                  {rule.algorithm.replace(/_/g, ' ')}
                </Badge>
              </div>
              <p className="text-sm text-muted-foreground">
                {rule.limitPerWindow} req / {rule.windowMs}ms
              </p>
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={() => setEditing(rule)}>
                  Edit
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  className="text-red-600"
                  onClick={() => handleDelete(rule.clientId)}
                >
                  Delete
                </Button>
              </div>
            </div>
          )
        )}

        {rules && rules.items.length === 0 && !adding && (
          <p className="text-sm text-muted-foreground text-center py-8">
            No rules yet. Add one to start rate limiting.
          </p>
        )}
      </main>
    </div>
  )
}
