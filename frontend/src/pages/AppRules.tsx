import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Plus, Route } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { RuleForm } from '@/components/RuleForm'
import { AppShell } from '@/components/AppShell'
import { useRules } from '@/hooks/useRules'
import { toast } from 'sonner'
import type { CreateRuleRequest, RuleResponse } from '@/types'

const ALGORITHM_COLORS: Record<string, string> = {
  FIXED_WINDOW: 'border-cyan-300 bg-cyan-50 text-cyan-900',
  TOKEN_BUCKET: 'border-amber-300 bg-amber-50 text-amber-900',
  SLIDING_WINDOW: 'border-emerald-300 bg-emerald-50 text-emerald-900',
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
    <AppShell
      eyebrow="Rule Builder"
      title="Rate-limit rules"
      description="Rules decide how each clientId is limited. Exact matches win first; wildcard patterns cover dynamic identities."
      action={
        <div className="flex flex-wrap gap-2">
          <Link to="/dashboard">
            <Button variant="outline" className="h-11 rounded-2xl bg-white/60">
              <ArrowLeft className="mr-1 size-4" />
              Dashboard
            </Button>
          </Link>
          {!adding && !editing && (
            <Button className="h-11 rounded-2xl px-5" onClick={() => setAdding(true)}>
              <Plus className="mr-1 size-4" />
              Add rule
            </Button>
          )}
        </div>
      }
    >
      <div className="mb-6 glass-panel rounded-3xl p-4">
        <p className="font-mono text-xs uppercase tracking-[0.18em] text-slate-500">App ID</p>
        <p className="mt-2 break-all font-mono text-sm text-slate-900">{appId}</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="space-y-4">
          {adding && (
            <RuleForm
              onSubmit={handleCreate}
              onCancel={() => setAdding(false)}
              pending={createPending}
            />
          )}

          {editing && (
            <RuleForm
              onSubmit={handleUpdate}
              onCancel={() => setEditing(null)}
              pending={updatePending}
              initial={editing}
            />
          )}

          {!adding && !editing && (
            <div className="ink-panel rounded-3xl p-5">
              <p className="font-heading text-2xl font-bold">Pattern matching</p>
              <p className="mt-3 text-sm leading-6 text-slate-300">
                Use <code>user:*</code> for all users, <code>ip:10.0.*</code> for a subnet, or <code>*</code> as a catch-all fallback. Throttlr still tracks each actual client independently.
              </p>
            </div>
          )}
        </div>

        <div className="space-y-3">
          {isLoading && <p className="text-sm text-slate-600">Loading rules...</p>}

          {rules?.items.map((rule) => (
            <div key={rule.ruleId} className="glass-panel rounded-3xl p-5 transition hover:-translate-y-0.5 hover:shadow-xl">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-2xl bg-slate-950 text-amber-200">
                    <Route className="size-5" />
                  </div>
                  <span className="break-all font-mono text-sm text-slate-950">{rule.clientId}</span>
                </div>
                <Badge className={ALGORITHM_COLORS[rule.algorithm] ?? ''} variant="outline">
                  {rule.algorithm.replace(/_/g, ' ')}
                </Badge>
              </div>
              <p className="mt-4 text-sm text-slate-600">
                {rule.limitPerWindow} requests / {rule.windowMs}ms
              </p>
              <div className="mt-4 flex gap-2">
                <Button size="sm" variant="outline" className="rounded-xl bg-white/60" onClick={() => setEditing(rule)}>
                  Edit
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  className="rounded-xl text-red-700"
                  onClick={() => handleDelete(rule.clientId)}
                >
                  Delete
                </Button>
              </div>
            </div>
          ))}

          {rules && rules.items.length === 0 && !adding && (
            <div className="glass-panel rounded-[2rem] p-10 text-center">
              <p className="font-heading text-2xl font-bold">No rules yet</p>
              <p className="mt-2 text-sm text-slate-600">Add one to start rate limiting this app.</p>
            </div>
          )}
        </div>
      </div>
    </AppShell>
  )
}
