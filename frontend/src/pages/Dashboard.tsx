import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { AppCard } from '@/components/AppCard'
import { OnetimeSecretModal } from '@/components/OnetimeSecretModal'
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

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b bg-white px-6 py-4 flex items-center justify-between">
        <span className="font-semibold text-lg">Throttlr</span>
        <Button variant="ghost" size="sm" onClick={logout}>
          Log out
        </Button>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-8 space-y-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-semibold">Apps</h1>
          {!creating && (
            <Button size="sm" onClick={() => setCreating(true)}>
              Create App
            </Button>
          )}
        </div>

        {creating && (
          <form onSubmit={handleCreate} className="flex gap-2">
            <Input
              placeholder="App name"
              value={appName}
              onChange={(e) => setAppName(e.target.value)}
              autoFocus
            />
            <Button type="submit" disabled={createPending || !appName.trim()}>
              {createPending ? 'Creating…' : 'Create'}
            </Button>
            <Button type="button" variant="ghost" onClick={() => setCreating(false)}>
              Cancel
            </Button>
          </form>
        )}

        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}

        {apps?.items.map((app) => (
          <AppCard key={app.appId} app={app} onDelete={handleDelete} />
        ))}

        {apps && apps.items.length === 0 && (
          <p className="text-sm text-muted-foreground text-center py-8">
            No apps yet. Create one to get started.
          </p>
        )}
      </main>

      {newApp && (
        <OnetimeSecretModal
          open={true}
          title="App Key Created"
          label="App key"
          secret={newApp.appKey}
          onConfirmed={() => setNewApp(null)}
        />
      )}
    </div>
  )
}
