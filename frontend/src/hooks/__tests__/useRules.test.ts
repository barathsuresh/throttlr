import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import React from 'react'
import { useRules } from '../useRules'

function wrapper({ children }: { children: React.ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return React.createElement(QueryClientProvider, { client }, children)
}

describe('useRules', () => {
  it('fetches rules for an app', async () => {
    const { result } = renderHook(() => useRules('app-1', 0), { wrapper })
    await waitFor(() => expect(result.current.rules).toBeDefined())
    expect(result.current.rules!.items[0].clientId).toBe('user:1')
  })
})
