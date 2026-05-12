import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import React from 'react'
import { useAuth } from '../useAuth'
import { clearToken, getToken } from '@/lib/token'

function wrapper({ children }: { children: React.ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return React.createElement(QueryClientProvider, { client }, children)
}

beforeEach(() => clearToken())

describe('useAuth', () => {
  it('isAuthenticated is false when no token', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    expect(result.current.isAuthenticated).toBe(false)
  })

  it('login mutation stores token', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {
      await result.current.login({ passphrase: 'test passphrase' })
    })
    await waitFor(() => expect(getToken()).toBe('test-jwt'))
  })

  it('logout clears token', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {
      await result.current.login({ passphrase: 'test passphrase' })
    })
    act(() => result.current.logout())
    expect(getToken()).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })
})
