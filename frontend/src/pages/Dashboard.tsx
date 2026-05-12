import { useState } from 'react'
import { Boxes, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { AppCard } from '@/components/AppCard'
import { OnetimeSecretModal } from '@/components/OnetimeSecretModal'
import { AppShell } from '@/components/AppShell'
import { useApps } from '@/hooks/useApps'
import { useAuth } from '@/hooks/useAuth'
import type { AppCreatedResponse } from '@/types'
import axios from 'axios'
import { toast } from 'sonner'

export function Dashboard() {
  const [page] = useState(0)
  const { apps, isLoading, createApp, createPending, deleteApp } = useApps(page)
  const { logout } = useAuth()

  const [creating, setCreating] = useState(false)
  const [appName, setAppName] = useState('')
  const [newApp, setNewApp] = useState<AppCreatedResponse | null>(null)

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!appName.trim()) return
    try {
      const data = await createApp({ name: appName.trim() })
      setNewApp(data)
      setAppName('')
      setCreating(false)
    } catch (err) {
      if (axios.isAxiosError(err)) {
        toast.error(err.response?.data?.message ?? 'Failed to create app')
      }
    }
  }

  const handleDelete = async (appId: string) => {
    try {
      await deleteApp(appId)
      toast.success('App deleted')
    } catch {
      toast.error('Failed to delete app')
    }
  }

  const totalRules = apps?.items.reduce((sum, app) => sum + app.ruleCount, 0) ?? 0

  return (
    <AppShell
      eyebrow="Developer Console"
      title="Apps"
      description="Create app keys, organize rules, and inspect current-hour analytics from one control surface."
      onLogout={logout}
      action={
        !creating && (
          <Button className="h-11 rounded-2xl px-5" onClick={() => setCreating(true)}>
            <Plus className="mr-1 size-4" />
            Create app
          </Button>
        )
      }
    >
      <section className="mb-6 grid gap-4 md:grid-cols-3">
        <div className="ink-panel rounded-3xl p-5">
          <p className="font-mono text-xs uppercase tracking-[0.18em] text-amber-200">Total apps</p>
          <p className="mt-3 font-heading text-4xl font-black">{apps?.totalItems ?? 0}</p>
        </div>
        <div className="glass-panel rounded-3xl p-5">
          <p className="font-mono text-xs uppercase tracking-[0.18em] text-slate-500">Rules tracked</p>
          <p className="mt-3 font-heading text-4xl font-black">{totalRules}</p>
        </div>
        <div className="glass-panel rounded-3xl p-5">
          <p className="font-mono text-xs uppercase tracking-[0.18em] text-slate-500">Runtime auth</p>
          <p className="mt-3 text-sm leading-6 text-slate-600">Use app keys for hot-path checks. JWT stays for dashboard-only operations.</p>
        </div>
      </section>

      {creating && (
        <form onSubmit={handleCreate} className="glass-panel mb-6 flex flex-col gap-3 rounded-3xl p-4 sm:flex-row">
          <Input
            className="h-11 rounded-2xl bg-white/70"
            placeholder="App name, e.g. Acme Gateway"
            value={appName}
            onChange={(e) => setAppName(e.target.value)}
            autoFocus
          />
          <Button className="h-11 rounded-2xl px-5" type="submit" disabled={createPending || !appName.trim()}>
            {createPending ? 'Creating...' : 'Create'}
          </Button>
          <Button className="h-11 rounded-2xl" type="button" variant="ghost" onClick={() => setCreating(false)}>
            Cancel
          </Button>
        </form>
      )}

      {isLoading && <p className="text-sm text-slate-600">Loading apps...</p>}

      <div className="grid gap-4 lg:grid-cols-2">
        {apps?.items.map((app) => (
          <AppCard key={app.appId} app={app} onDelete={handleDelete} />
        ))}
      </div>

      {apps && apps.items.length === 0 && (
        <div className="glass-panel grid place-items-center rounded-[2rem] p-12 text-center">
          <Boxes className="mb-4 size-10 text-slate-500" />
          <p className="font-heading text-2xl font-bold">No apps yet</p>
          <p className="mt-2 max-w-md text-sm leading-6 text-slate-600">
            Create your first app to receive a one-time app key and start adding rate-limit rules.
          </p>
        </div>
      )}

      {newApp && (
        <OnetimeSecretModal
          key={newApp.appId}
          open={true}
          title="App Key Created"
          label="App key"
          secret={newApp.appKey}
          onConfirmed={() => setNewApp(null)}
        />
      )}
    </AppShell>
  )
}
