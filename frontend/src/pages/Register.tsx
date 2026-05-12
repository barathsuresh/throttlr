import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { OnetimeSecretModal } from '@/components/OnetimeSecretModal'
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
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Create account</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            No password needed. Throttlr generates a BIP39 passphrase — your only credential.
          </p>
          <Button className="w-full" onClick={handleRegister} disabled={registerPending}>
            {registerPending ? 'Creating…' : 'Create account'}
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            Already have an account?{' '}
            <Link to="/login" className="underline">
              Log in
            </Link>
          </p>
        </CardContent>
      </Card>

      {secret && (
        <OnetimeSecretModal
          open={true}
          title="Your Passphrase"
          label="Passphrase"
          secret={secret.passphrase}
          onConfirmed={() => navigateTo('/dashboard')}
        />
      )}
    </div>
  )
}
