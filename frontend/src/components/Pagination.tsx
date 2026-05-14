import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface Props {
  page: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, hasNext, hasPrevious, onPageChange }: Props) {
  if (totalPages <= 1) return null

  return (
    <div className="flex items-center justify-center gap-3 pt-4">
      <Button
        variant="outline"
        size="sm"
        className="rounded-xl bg-white/60 dark:bg-slate-800/60 dark:border-white/10 dark:text-slate-300"
        disabled={!hasPrevious}
        onClick={() => onPageChange(page - 1)}
      >
        <ChevronLeft className="size-4" />
        Prev
      </Button>
      <span className="font-mono text-xs text-slate-500 dark:text-slate-400">
        {page + 1} / {totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        className="rounded-xl bg-white/60 dark:bg-slate-800/60 dark:border-white/10 dark:text-slate-300"
        disabled={!hasNext}
        onClick={() => onPageChange(page + 1)}
      >
        Next
        <ChevronRight className="size-4" />
      </Button>
    </div>
  )
}
