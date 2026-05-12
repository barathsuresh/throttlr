import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'

const STACK = ['Spring Boot 4', 'Java 25', 'Redis', 'MongoDB', 'JWT', 'BIP39']

export function Landing() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <header className="border-b bg-white px-6 py-4 flex items-center justify-between">
        <span className="font-semibold text-lg">Throttlr</span>
        <div className="flex gap-2">
          <Link to="/demo">
            <Button variant="outline" size="sm">Try Demo</Button>
          </Link>
          <Link to="/login">
            <Button size="sm">Log in</Button>
          </Link>
        </div>
      </header>

      <main className="flex-1 flex flex-col items-center justify-center text-center px-4 space-y-8">
        <div className="space-y-3 max-w-xl">
          <h1 className="text-4xl font-bold tracking-tight">Self-serve rate limiting</h1>
          <p className="text-lg text-muted-foreground">
            Throttlr is a rate-limiting engine. Register an app, define rules — fixed window, token bucket, or sliding window — and protect your API with a single header.
          </p>
        </div>

        <div className="flex gap-3">
          <Link to="/demo">
            <Button size="lg" variant="outline">Try the live demo →</Button>
          </Link>
          <Link to="/register">
            <Button size="lg">Get started</Button>
          </Link>
        </div>

        <div className="flex flex-wrap justify-center gap-2">
          {STACK.map((tech) => (
            <Badge key={tech} variant="secondary">{tech}</Badge>
          ))}
        </div>
      </main>

      <footer className="border-t bg-white px-6 py-4 text-center text-xs text-muted-foreground">
        Rate-limit decisions are atomic via Redis Lua scripts. Analytics are counted in Redis and snapshotted hourly to MongoDB.
      </footer>
    </div>
  )
}
