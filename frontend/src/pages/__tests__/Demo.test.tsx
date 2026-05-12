import { describe, it, expect } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { render } from '@/test/utils'
import { Demo } from '../Demo'

describe('Demo', () => {
  it('renders three algorithm tabs after loading', async () => {
    render(<Demo />, { initialEntries: ['/demo'] })
    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /fixed window/i })).toBeInTheDocument()
      expect(screen.getByRole('tab', { name: /token bucket/i })).toBeInTheDocument()
      expect(screen.getByRole('tab', { name: /sliding window/i })).toBeInTheDocument()
    })
  })

  it('shows Send Request button for each tab', async () => {
    render(<Demo />, { initialEntries: ['/demo'] })
    await waitFor(() => screen.getByRole('tab', { name: /fixed window/i }))
    expect(screen.getByRole('button', { name: /send request/i })).toBeInTheDocument()
  })
})
