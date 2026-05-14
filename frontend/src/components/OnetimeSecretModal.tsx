import { useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

interface Props {
  open: boolean
  title: string
  label: string
  secret: string
  onConfirmed: () => void
}

export function OnetimeSecretModal({ open, title, label, secret, onConfirmed }: Props) {
  const [copied, setCopied] = useState(false)
  const [confirmed, setConfirmed] = useState(false)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(secret)
    setCopied(true)
  }

  return (
    <Dialog open={open} onOpenChange={() => {}} disablePointerDismissal>
      <DialogContent className="sm:max-w-md" showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>

        <p className="text-sm text-muted-foreground">
          This {label.toLowerCase()} will not be shown again. Copy it now and store it safely.
        </p>

        <div className="relative rounded-md border border-border bg-slate-50 p-3 font-mono text-sm break-all text-slate-900 dark:bg-slate-800 dark:text-slate-100 dark:border-white/10">
          {secret}
          <button
            onClick={handleCopy}
            className="absolute top-2 right-2 text-xs text-slate-500 hover:text-slate-800 underline dark:text-slate-400 dark:hover:text-slate-200"
          >
            {copied ? 'Copied!' : 'Copy'}
          </button>
        </div>

        <label className="flex items-center gap-2 text-sm cursor-pointer select-none text-popover-foreground">
          <input
            type="checkbox"
            role="checkbox"
            checked={confirmed}
            onChange={(e) => setConfirmed(e.target.checked)}
            className="h-4 w-4"
          />
          I've saved my {label.toLowerCase()} somewhere safe
        </label>

        <DialogFooter>
          <Button onClick={onConfirmed} disabled={!confirmed}>
            I've saved it — continue
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
