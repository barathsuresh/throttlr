import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import React from 'react'
import { useBackendHealth } from '../useBackendHealth'

function wrapper({ children }: { children: React.ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return React.createElement(QueryClientProvider, { client }, children)
}

describe('useBackendHealth', () => {
  it('returns isAlive=true when health endpoint returns 200', async () => {
    const { result } = renderHook(() => useBackendHealth(), { wrapper })
    await waitFor(() => expect(result.current.isAlive).toBe(true))
  })
})
