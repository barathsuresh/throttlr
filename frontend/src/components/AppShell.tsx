import { Link, NavLink } from 'react-router-dom'
import type { ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { getToken } from '@/lib/token'
import { cn } from '@/lib/utils'

interface AppShellProps {
  children: ReactNode
  eyebrow?: string
  title?: string
  description?: string
  action?: ReactNode
  compact?: boolean
  onLogout?: () => void
}

const NAV_ITEMS = [
  { to: '/demo', label: 'Demo' },
  { to: '/docs', label: 'Docs' },
  { to: '/dashboard', label: 'Dashboard' },
]

export function AppShell({
  children,
  eyebrow,
  title,
  description,
  action,
  compact = false,
  onLogout,
}: AppShellProps) {
  const authed = getToken()

  return (
    <div className="relative min-h-screen overflow-hidden shell-grid">
      <div className="pointer-events-none absolute -left-28 top-24 h-72 w-72 rounded-full bg-amber-300/30 blur-3xl" />
      <div className="pointer-events-none absolute right-0 top-0 h-80 w-80 rounded-full bg-cyan-300/20 blur-3xl" />

      <header className="sticky top-0 z-30 border-b border-slate-900/10 bg-[#fbf1d8]/85 px-4 py-3 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4">
          <Link to="/" className="flex items-center gap-3">
            <span className="grid h-9 w-9 place-items-center rounded-2xl bg-slate-950 text-sm font-black text-amber-300 shadow-lg shadow-slate-900/20">
              T
            </span>
            <span className="font-heading text-xl font-bold tracking-tight">Throttlr</span>
          </Link>

          <nav className="hidden items-center rounded-full border border-slate-900/10 bg-white/55 p-1 text-sm shadow-sm md:flex">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'rounded-full px-4 py-2 text-slate-600 transition hover:text-slate-950',
                    isActive && 'bg-slate-950 text-amber-200 shadow-sm hover:text-amber-200'
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            {authed ? (
              <Button variant="ghost" size="sm" onClick={onLogout}>
                Log out
              </Button>
            ) : (
              <>
                <Link to="/login">
                  <Button variant="ghost" size="sm">Log in</Button>
                </Link>
                <Link to="/register" className="hidden sm:block">
                  <Button size="sm">Start free</Button>
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main className={cn('relative z-10 mx-auto w-full max-w-7xl px-4 py-8 md:px-6', compact ? 'max-w-4xl' : '')}>
        {(title || description || action) && (
          <section className="mb-8 flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
            <div className="max-w-3xl animate-rise">
              {eyebrow && (
                <p className="mb-3 font-mono text-xs font-semibold uppercase tracking-[0.24em] text-slate-600">
                  {eyebrow}
                </p>
              )}
              {title && (
                <h1 className="font-heading text-4xl font-black tracking-[-0.04em] text-slate-950 md:text-6xl">
                  {title}
                </h1>
              )}
              {description && (
                <p className="mt-4 max-w-2xl text-base leading-7 text-slate-600 md:text-lg">
                  {description}
                </p>
              )}
            </div>
            {action && <div className="animate-rise">{action}</div>}
          </section>
        )}

        {children}
      </main>
    </div>
  )
}
