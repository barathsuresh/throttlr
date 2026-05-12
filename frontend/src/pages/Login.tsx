import { useState } from 'react'
import { Link } from 'react-router-dom'
import { KeyRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { AppShell } from '@/components/AppShell'
import { useAuth } from '@/hooks/useAuth'
import axios from 'axios'

export function Login() {
  const { login, loginPending } = useAuth()
  const [passphrase, setPassphrase] = useState('')
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await login({ passphrase })
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message ?? 'Login failed')
      }
    }
  }

  return (
    <AppShell compact>
      <div className="mx-auto grid min-h-[calc(100vh-10rem)] max-w-5xl items-center gap-8 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="animate-rise">
          <p className="font-mono text-xs font-semibold uppercase tracking-[0.24em] text-slate-600">Welcome back</p>
          <h1 className="mt-4 font-heading text-5xl font-black tracking-[-0.05em] text-slate-950 md:text-7xl">
            Enter the control room.
          </h1>
          <p className="mt-5 text-lg leading-8 text-slate-600">
            Throttlr uses your BIP39 passphrase to mint a stateless JWT. No password reset flow, so store the passphrase safely.
          </p>
        </div>

        <Card className="glass-panel rounded-[2rem] py-6">
          <CardHeader>
            <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-amber-200">
              <KeyRound className="size-5" />
            </div>
            <CardTitle className="font-heading text-2xl font-bold">Log in</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="passphrase">Passphrase</Label>
                <Textarea
                  id="passphrase"
                  className="min-h-28 rounded-2xl bg-white/70"
                  placeholder="Enter your 12-word BIP39 passphrase"
                  rows={4}
                  value={passphrase}
                  onChange={(e) => setPassphrase(e.target.value)}
                />
                {error && <p className="text-sm text-red-700">{error}</p>}
              </div>
              <Button type="submit" className="h-11 w-full rounded-2xl" disabled={loginPending || !passphrase.trim()}>
                {loginPending ? 'Logging in...' : 'Log in'}
              </Button>
              <p className="text-center text-sm text-slate-600">
                No account?{' '}
                <Link to="/register" className="font-medium underline underline-offset-4">
                  Register
                </Link>
              </p>
            </form>
          </CardContent>
        </Card>
      </div>
    </AppShell>
  )
}
