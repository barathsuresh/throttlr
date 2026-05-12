import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { login as loginApi, register as registerApi } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/lib/token'
import { navigateTo } from '@/lib/navigate'
import type { LoginRequest } from '@/types'

export function useAuth() {
  const qc = useQueryClient()
  const [, forceUpdate] = useState(0)

  const loginMutation = useMutation({
    mutationFn: (body: LoginRequest) => loginApi(body),
    onSuccess: (data) => {
      setToken(data.sessionToken)
      forceUpdate((n) => n + 1)
      navigateTo('/dashboard')
    },
  })

  const registerMutation = useMutation({
    mutationFn: () => registerApi(),
  })

  const logout = () => {
    clearToken()
    qc.clear()
    forceUpdate((n) => n + 1)
    navigateTo('/login')
  }

  return {
    isAuthenticated: !!getToken(),
    login: loginMutation.mutateAsync,
    loginPending: loginMutation.isPending,
    loginError: loginMutation.error,
    register: registerMutation.mutateAsync,
    registerPending: registerMutation.isPending,
    registerError: registerMutation.error,
    logout,
  }
}
