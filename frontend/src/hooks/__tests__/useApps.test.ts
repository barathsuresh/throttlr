import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import React from 'react'
import { useApps } from '../useApps'

function wrapper({ children }: { children: React.ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return React.createElement(QueryClientProvider, { client }, children)
}

describe('useApps', () => {
  it('fetches apps list', async () => {
    const { result } = renderHook(() => useApps(0), { wrapper })
    await waitFor(() => expect(result.current.apps).toBeDefined())
    expect(result.current.apps!.items[0].appId).toBe('app-1')
  })
})
