import { useEffect } from 'react'
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom'
import { setNavigate } from '@/lib/navigate'
import { getToken } from '@/lib/token'
import { Landing } from '@/pages/Landing'
import { Login } from '@/pages/Login'
import { Register } from '@/pages/Register'
import { Dashboard } from '@/pages/Dashboard'
import { AppRules } from '@/pages/AppRules'
import { Demo } from '@/pages/Demo'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  return getToken() ? <>{children}</> : <Navigate to="/login" replace />
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
