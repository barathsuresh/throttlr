import { describe, it, expect } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { render } from '@/test/utils'
import { Dashboard } from '../Dashboard'

describe('Dashboard', () => {
  it('renders app list', async () => {
    render(<Dashboard />, { initialEntries: ['/dashboard'] })
    await waitFor(() => expect(screen.getByText('Test App')).toBeInTheDocument())
  })

  it('shows create app form when button clicked', async () => {
    const user = userEvent.setup()
    render(<Dashboard />, { initialEntries: ['/dashboard'] })
    await waitFor(() => screen.getByRole('button', { name: /create app/i }))
    await user.click(screen.getByRole('button', { name: /create app/i }))
    expect(screen.getByPlaceholderText(/app name/i)).toBeInTheDocument()
  })
})
