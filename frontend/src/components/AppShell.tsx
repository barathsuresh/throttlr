import { Link, NavLink } from 'react-router-dom'
import type { ReactNode } from 'react'
import { Moon, Sun } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { getToken } from '@/lib/token'
import { useAuth } from '@/hooks/useAuth'
import { useDarkMode } from '@/hooks/useDarkMode'
import { cn } from '@/lib/utils'

interface AppShellProps {
  children: ReactNode
  eyebrow?: string
  title?: string
  description?: string
  action?: ReactNode
  compact?: boolean
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
}: AppShellProps) {
  const authed = getToken()
  const { logout } = useAuth()
  const { dark, toggle } = useDarkMode()

  return (
    <div className="relative min-h-screen overflow-hidden shell-grid">
      <div className="pointer-events-none absolute -left-28 top-24 h-72 w-72 rounded-full bg-amber-300/30 blur-3xl" />
      <div className="pointer-events-none absolute right-0 top-0 h-80 w-80 rounded-full bg-cyan-300/20 blur-3xl" />

      <header className="sticky top-0 z-30 border-b border-slate-900/10 bg-[#fbf1d8]/85 px-4 py-3 backdrop-blur-xl dark:border-white/10 dark:bg-slate-900/85">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4">
          <Link to="/" className="flex items-center gap-3">
            <span className="grid h-9 w-9 place-items-center rounded-2xl bg-slate-950 shadow-lg shadow-slate-900/20">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 289.0625 30 30" fill="#fcd34d" className="h-5 w-5">
                <path d="m 14.703125,5.5722656 a 12,12 0 0 0 -12,12.0000004 12,12 0 0 0 0.6894531,3.964843 A 10.746539,10.746539 0 0 1 3.0976562,19.09375 10.746539,10.746539 0 0 1 13.84375,8.3476562 10.746539,10.746539 0 0 1 19.826172,10.173828 L 21.966797,8.0332031 A 12,12 0 0 0 14.703125,5.5722656 Z m 9.148437,3.6035156 c -0.25173,0.00423 -0.507325,0.1022801 -0.71875,0.28125 l -9.193359,7.7812498 c -0.422789,0.35795 -1.416166,1.411952 -0.002,2.826172 1.414471,1.41445 2.468093,0.418804 2.826172,-0.0039 l 7.783203,-9.189453 c 0.358051,-0.42275 0.391737,-1.0223328 0,-1.4140628 -0.195869,-0.19587 -0.443582,-0.285475 -0.695313,-0.28125 z m 1.84961,3.6074218 -2.021484,2.021485 A 10.746539,10.746539 0 0 1 24.585938,19 h 2.015624 a 12,12 0 0 0 0.101563,-1.427734 12,12 0 0 0 -1.001953,-4.789063 z" transform="translate(0,289.0625)"/>
              </svg>
            </span>
            <span className="font-heading text-xl font-bold tracking-tight">Throttlr</span>
          </Link>

          <nav className="hidden items-center rounded-full border border-slate-900/10 bg-white/55 p-1 text-sm shadow-sm md:flex dark:border-white/10 dark:bg-slate-800/60">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'rounded-full px-4 py-2 text-slate-600 transition hover:text-slate-950 dark:text-slate-300 dark:hover:text-slate-50',
                    isActive && 'bg-slate-950 text-amber-200 shadow-sm hover:text-amber-200 dark:bg-amber-300 dark:text-slate-950 dark:hover:text-slate-950'
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" onClick={toggle} aria-label="Toggle dark mode">
              {dark ? <Sun className="size-4" /> : <Moon className="size-4" />}
            </Button>
            {authed ? (
              <Button variant="ghost" size="sm" onClick={logout}>
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
                <p className="mb-3 font-mono text-xs font-semibold uppercase tracking-[0.24em] text-slate-600 dark:text-slate-400">
                  {eyebrow}
                </p>
              )}
              {title && (
                <h1 className="font-heading text-4xl font-black tracking-[-0.04em] text-slate-950 dark:text-slate-50 md:text-6xl">
                  {title}
                </h1>
              )}
              {description && (
                <p className="mt-4 max-w-2xl text-base leading-7 text-slate-600 dark:text-slate-400 md:text-lg">
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
