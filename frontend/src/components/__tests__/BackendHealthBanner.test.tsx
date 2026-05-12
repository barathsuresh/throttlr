import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { render } from '@/test/utils'
import { BackendHealthBanner } from '../BackendHealthBanner'

describe('BackendHealthBanner', () => {
  it('renders nothing when backend is alive', () => {
    render(<BackendHealthBanner isAlive={true} />)
    expect(screen.queryByText(/waking up/i)).toBeNull()
  })

  it('renders wake-up banner when backend is not alive', () => {
    render(<BackendHealthBanner isAlive={false} />)
    expect(screen.getByText(/waking up/i)).toBeInTheDocument()
  })
})
