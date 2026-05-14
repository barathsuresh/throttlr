import { useEffect } from 'react'
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { setNavigate } from '@/lib/navigate'
import { getToken, clearToken } from '@/lib/token'
import { me } from '@/api/auth'
import { Landing } from '@/pages/Landing'
import { Login } from '@/pages/Login'
import { Register } from '@/pages/Register'
import { Dashboard } from '@/pages/Dashboard'
import { AppRules } from '@/pages/AppRules'
import { Demo } from '@/pages/Demo'
import { Docs } from '@/pages/Docs'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = getToken()

  const { isLoading, isError } = useQuery({
    queryKey: ['me'],
    queryFn: me,
    enabled: !!token,
    retry: false,
    staleTime: 5 * 60 * 1000,
  })

  if (!token) return <Navigate to="/login" replace />

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <span className="font-mono text-xs text-slate-400">Verifying session…</span>
      </div>
    )
  }

  if (isError) {
    clearToken()
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}

function PublicOnlyRoute({ children }: { children: React.ReactNode }) {
  return !getToken() ? <>{children}</> : <Navigate to="/dashboard" replace />
}

export function AppRouter() {
  const navigate = useNavigate()

  useEffect(() => {
    setNavigate(navigate)
  }, [navigate])

  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/demo" element={<Demo />} />
      <Route path="/docs" element={<Docs />} />
      <Route
        path="/login"
        element={<PublicOnlyRoute><Login /></PublicOnlyRoute>}
      />
      <Route
        path="/register"
        element={<PublicOnlyRoute><Register /></PublicOnlyRoute>}
      />
      <Route
        path="/dashboard"
        element={<ProtectedRoute><Dashboard /></ProtectedRoute>}
      />
      <Route
        path="/apps/:appId/rules"
        element={<ProtectedRoute><AppRules /></ProtectedRoute>}
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
