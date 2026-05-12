import { BrowserRouter } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import { queryClient } from '@/lib/queryClient'
import { AppRouter } from '@/router'
import { useBackendHealth } from '@/hooks/useBackendHealth'
import { BackendHealthBanner } from '@/components/BackendHealthBanner'

function AppInner() {
  const { isAlive } = useBackendHealth()
  return (
    <>
      <BackendHealthBanner isAlive={isAlive} />
      <AppRouter />
    </>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppInner />
        <Toaster richColors position="bottom-right" />
      </BrowserRouter>
    </QueryClientProvider>
  )
}
