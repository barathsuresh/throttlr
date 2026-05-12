import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ShieldPlus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { OnetimeSecretModal } from '@/components/OnetimeSecretModal'
import { AppShell } from '@/components/AppShell'
import { useAuth } from '@/hooks/useAuth'
import { navigateTo } from '@/lib/navigate'
import type { RegisterResponse } from '@/types'

export function Register() {
  const { register, registerPending } = useAuth()
  const [secret, setSecret] = useState<RegisterResponse | null>(null)

  const handleRegister = async () => {
    const data = await register()
    setSecret(data)
  }

  return (
    <AppShell compact>
      <div className="mx-auto grid min-h-[calc(100vh-10rem)] max-w-5xl items-center gap-8 lg:grid-cols-[1.05fr_0.95fr]">
        <div className="animate-rise">
          <p className="font-mono text-xs font-semibold uppercase tracking-[0.24em] text-slate-600">One-time credential</p>
          <h1 className="mt-4 font-heading text-5xl font-black tracking-[-0.05em] text-slate-950 md:text-7xl">
            Create an account without a password.
          </h1>
          <p className="mt-5 text-lg leading-8 text-slate-600">
            Throttlr generates a BIP39 passphrase and stores only a secure lookup plus password hash. The raw phrase is shown once.
          </p>
        </div>

        <Card className="glass-panel rounded-[2rem] py-6">
          <CardHeader>
            <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-amber-200">
              <ShieldPlus className="size-5" />
            </div>
            <CardTitle className="font-heading text-2xl font-bold">Create account</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm leading-6 text-slate-600">
              No email, no password. Store the generated passphrase somewhere safe because it cannot be retrieved later.
            </p>
            <Button className="h-11 w-full rounded-2xl" onClick={handleRegister} disabled={registerPending}>
              {registerPending ? 'Creating...' : 'Generate passphrase'}
            </Button>
            <p className="text-center text-sm text-slate-600">
              Already have an account?{' '}
              <Link to="/login" className="font-medium underline underline-offset-4">
                Log in
              </Link>
            </p>
          </CardContent>
        </Card>
      </div>

      {secret && (
        <OnetimeSecretModal
          open={true}
          title="Your Passphrase"
          label="Passphrase"
          secret={secret.passphrase}
          onConfirmed={() => navigateTo('/dashboard')}
        />
      )}
    </AppShell>
  )
}
