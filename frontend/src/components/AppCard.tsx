import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BarChart3, KeyRound, ShieldCheck, Trash2 } from 'lucide-react'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { AnalyticsPanel } from './AnalyticsPanel'
import { OnetimeSecretModal } from './OnetimeSecretModal'
import type { AppCreatedResponse, AppResponse } from '@/types'
import { toast } from 'sonner'

function formatDate(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

interface Props {
  app: AppResponse
  onDelete: (appId: string) => void
  onRotateKey: (appId: string) => Promise<AppCreatedResponse>
}

export function AppCard({ app, onDelete, onRotateKey }: Props) {
  const navigate = useNavigate()
  const [showAnalytics, setShowAnalytics] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [confirmingRotate, setConfirmingRotate] = useState(false)
  const [rotatePending, setRotatePending] = useState(false)
  const [newKey, setNewKey] = useState<AppCreatedResponse | null>(null)

  const handleRotate = async () => {
    setRotatePending(true)
    try {
      const result = await onRotateKey(app.appId)
      setConfirmingRotate(false)
      setNewKey(result)
    } catch {
      toast.error('Failed to rotate key')
    } finally {
      setRotatePending(false)
    }
  }

  return (
    <Card className="glass-panel rounded-3xl border-white/60 bg-white/65 py-5">
      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-2">
        <div>
          <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-2xl bg-slate-950 text-amber-200">
            <ShieldCheck className="size-5" />
          </div>
          <p className="font-heading text-2xl font-bold tracking-tight dark:text-slate-50">{app.name}</p>
          <p className="mt-1 break-all font-mono text-xs text-slate-500 dark:text-slate-400">{app.appId}</p>
          <p className="mt-1 font-mono text-xs text-slate-400 dark:text-slate-500">Created {formatDate(app.createdAt)}</p>
        </div>
        <Badge variant="secondary" className="rounded-full bg-amber-100 text-slate-800 dark:bg-amber-900/40 dark:text-amber-200">
          {app.ruleCount} rule{app.ruleCount !== 1 ? 's' : ''}
        </Badge>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex gap-2 flex-wrap">
          <Button size="sm" className="rounded-xl" onClick={() => navigate(`/apps/${app.appId}/rules`)}>
            Manage rules
          </Button>
          <Button
            size="sm"
            variant="outline"
            className="rounded-xl bg-white/60 dark:bg-slate-800/60 dark:border-white/10 dark:text-slate-300"
            onClick={() => setShowAnalytics((v) => !v)}
          >
            <BarChart3 className="mr-1 size-3.5" />
            {showAnalytics ? 'Hide analytics' : 'Show analytics'}
          </Button>

          {!confirming && !confirmingRotate && (
            <Button
              size="sm"
              variant="ghost"
              className="rounded-xl text-amber-700 hover:text-amber-800 dark:text-amber-400 dark:hover:text-amber-300"
              onClick={() => setConfirmingRotate(true)}
            >
              <KeyRound className="mr-1 size-3.5" />
              Rotate key
            </Button>
          )}

          {confirmingRotate && (
            <>
              <Button
                size="sm"
                variant="outline"
                className="rounded-xl border-amber-300 bg-amber-50 text-amber-800 hover:bg-amber-100 dark:border-amber-700 dark:bg-amber-900/20 dark:text-amber-300"
                disabled={rotatePending}
                onClick={handleRotate}
              >
                {rotatePending ? 'Rotating...' : 'Confirm rotate'}
              </Button>
              <Button size="sm" variant="ghost" className="rounded-xl" onClick={() => setConfirmingRotate(false)}>
                Cancel
              </Button>
            </>
          )}

          {!confirming && !confirmingRotate && (
            <Button size="sm" variant="ghost" className="rounded-xl text-red-700 hover:text-red-800" onClick={() => setConfirming(true)}>
              <Trash2 className="mr-1 size-3.5" />
              Delete
            </Button>
          )}

          {confirming && (
            <>
              <Button size="sm" variant="destructive" className="rounded-xl" onClick={() => onDelete(app.appId)}>
                Confirm delete
              </Button>
              <Button size="sm" variant="ghost" className="rounded-xl" onClick={() => setConfirming(false)}>
                Cancel
              </Button>
            </>
          )}
        </div>
        {showAnalytics && <AnalyticsPanel appId={app.appId} />}
      </CardContent>

      {newKey && (
        <OnetimeSecretModal
          key={newKey.appId + '-rotated'}
          open={true}
          title="New App Key"
          label="App key"
          secret={newKey.appKey}
          onConfirmed={() => setNewKey(null)}
        />
      )}
    </Card>
  )
}
