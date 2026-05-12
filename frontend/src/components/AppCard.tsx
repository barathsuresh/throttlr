import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BarChart3, ShieldCheck, Trash2 } from 'lucide-react'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { AnalyticsPanel } from './AnalyticsPanel'
import type { AppResponse } from '@/types'

interface Props {
  app: AppResponse
  onDelete: (appId: string) => void
}

export function AppCard({ app, onDelete }: Props) {
  const navigate = useNavigate()
  const [showAnalytics, setShowAnalytics] = useState(false)
  const [confirming, setConfirming] = useState(false)

  return (
    <Card className="glass-panel rounded-3xl border-white/60 bg-white/65 py-5">
      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-2">
        <div>
          <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-2xl bg-slate-950 text-amber-200">
            <ShieldCheck className="size-5" />
          </div>
          <p className="font-heading text-2xl font-bold tracking-tight">{app.name}</p>
          <p className="mt-1 break-all font-mono text-xs text-slate-500">{app.appId}</p>
        </div>
        <Badge variant="secondary" className="rounded-full bg-amber-100 text-slate-800">
          {app.ruleCount} rule{app.ruleCount !== 1 ? 's' : ''}
        </Badge>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex gap-2 flex-wrap">
          <Button size="sm" className="rounded-xl" onClick={() => navigate(`/apps/${app.appId}/rules`)}>
            Manage rules
          </Button>
          <Button size="sm" variant="outline" className="rounded-xl bg-white/60" onClick={() => setShowAnalytics((v) => !v)}>
            <BarChart3 className="mr-1 size-3.5" />
            {showAnalytics ? 'Hide analytics' : 'Show analytics'}
          </Button>
          {!confirming ? (
            <Button size="sm" variant="ghost" className="rounded-xl text-red-700 hover:text-red-800" onClick={() => setConfirming(true)}>
              <Trash2 className="mr-1 size-3.5" />
              Delete
            </Button>
          ) : (
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
    </Card>
  )
}
