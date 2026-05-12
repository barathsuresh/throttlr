import { Link } from 'react-router-dom'
import { ArrowRight, Gauge, KeyRound, LockKeyhole, Radar } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { AppShell } from '@/components/AppShell'

const STACK = ['Spring Boot', 'Redis Lua', 'MongoDB', 'JWT', 'BIP39', 'Cloud Run']

const FEATURES = [
  {
    icon: Gauge,
    title: 'Atomic decisions',
    copy: 'Every limit check runs in Redis with Lua, so counters stay correct under concurrency.',
  },
  {
    icon: KeyRound,
    title: 'Self-serve app keys',
    copy: 'Developers create apps, receive one-time keys, and attach rules without operator work.',
  },
  {
    icon: Radar,
    title: 'Exact + wildcard rules',
    copy: 'Match user:123 directly or cover dynamic identities with patterns like user:* and *.',
  },
]

export function Landing() {
  return (
    <AppShell>
      <section className="grid min-h-[calc(100vh-9rem)] items-center gap-10 py-10 lg:grid-cols-[1.05fr_0.95fr]">
        <div className="max-w-3xl animate-rise">
          <Badge className="mb-5 border border-slate-900/10 bg-white/70 px-3 py-1 text-slate-700">
            Redis hot path • Mongo cache fallback • JWT dashboard
          </Badge>
          <h1 className="font-heading text-6xl font-black leading-[0.92] tracking-[-0.07em] text-slate-950 md:text-8xl">
            Rate limits with teeth.
          </h1>
          <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600 md:text-xl">
            Throttlr is a self-serve rate limiting engine for developers. Create an app, define exact or wildcard rules, and protect APIs with a single app-key header.
          </p>
          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <Link to="/register">
              <Button size="lg" className="h-12 rounded-2xl px-5">
                Start building <ArrowRight className="ml-1 size-4" />
              </Button>
            </Link>
            <Link to="/demo">
              <Button size="lg" variant="outline" className="h-12 rounded-2xl border-slate-900/15 bg-white/60 px-5">
                Try live demo
              </Button>
            </Link>
            <Link to="/docs">
              <Button size="lg" variant="ghost" className="h-12 rounded-2xl px-5">
                Read docs
              </Button>
            </Link>
          </div>
          <div className="mt-8 flex flex-wrap gap-2">
            {STACK.map((tech) => (
              <Badge key={tech} variant="secondary" className="rounded-full bg-white/70 px-3 py-1 text-slate-700">
                {tech}
              </Badge>
            ))}
          </div>
        </div>

        <div className="relative animate-rise lg:pl-6">
          <div className="absolute -right-8 -top-8 h-28 w-28 rounded-full bg-amber-300/60 blur-2xl" />
          <div className="ink-panel animate-float-slow overflow-hidden rounded-[2rem] p-5">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div>
                <p className="font-mono text-xs uppercase tracking-[0.18em] text-amber-200">Live decision</p>
                <p className="font-heading text-2xl font-bold">/api/check</p>
              </div>
              <LockKeyhole className="size-6 text-emerald-300" />
            </div>
            <div className="mt-5 space-y-3 font-mono text-sm">
              <div className="rounded-2xl bg-white/5 p-4 text-slate-300">
                <span className="text-cyan-200">X-App-Key</span>: throttlr_live_...
              </div>
              <div className="rounded-2xl bg-white/5 p-4 text-slate-300">
                {"{ \"clientId\": \"user:alex\", \"rule\": \"user:*\" }"}
              </div>
              <div className="rounded-2xl border border-emerald-300/30 bg-emerald-300/10 p-4">
                <p className="text-emerald-200">ALLOWED</p>
                <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/10">
                  <div className="h-full w-2/3 rounded-full bg-gradient-to-r from-emerald-300 to-amber-200" />
                </div>
                <p className="mt-3 text-slate-300">remaining=67 resetAfterMs=42000</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 pb-12 md:grid-cols-3">
        {FEATURES.map((feature) => (
          <div key={feature.title} className="glass-panel rounded-3xl p-5 transition hover:-translate-y-1 hover:shadow-2xl">
            <feature.icon className="mb-5 size-7 text-slate-900" />
            <h2 className="font-heading text-2xl font-bold tracking-tight">{feature.title}</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">{feature.copy}</p>
          </div>
        ))}
      </section>
    </AppShell>
  )
}
