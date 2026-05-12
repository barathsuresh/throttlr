import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
    <Card>
      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-2">
        <div>
          <p className="font-medium">{app.name}</p>
          <p className="text-xs text-muted-foreground font-mono">{app.appId}</p>
        </div>
        <Badge variant="secondary">{app.ruleCount} rule{app.ruleCount !== 1 ? 's' : ''}</Badge>
      </CardHeader>
      <CardContent className="space-y-2">
        <div className="flex gap-2 flex-wrap">
          <Button size="sm" variant="outline" onClick={() => navigate(`/apps/${app.appId}/rules`)}>
            Manage rules
          </Button>
          <Button size="sm" variant="ghost" onClick={() => setShowAnalytics((v) => !v)}>
            {showAnalytics ? 'Hide analytics' : 'Show analytics'}
          </Button>
          {!confirming ? (
            <Button size="sm" variant="ghost" className="text-red-600 hover:text-red-700" onClick={() => setConfirming(true)}>
              Delete
            </Button>
          ) : (
            <>
              <Button size="sm" variant="destructive" onClick={() => onDelete(app.appId)}>
                Confirm delete
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setConfirming(false)}>
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
