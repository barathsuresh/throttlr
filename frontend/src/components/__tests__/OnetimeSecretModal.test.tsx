import { describe, it, expect, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { render } from '@/test/utils'
import { OnetimeSecretModal } from '../OnetimeSecretModal'

describe('OnetimeSecretModal', () => {
  it('renders the secret value', () => {
    render(
      <OnetimeSecretModal
        open={true}
        title="Your Passphrase"
        label="Passphrase"
        secret="word1 word2 word3"
        onConfirmed={vi.fn()}
      />
    )
    expect(screen.getByText('word1 word2 word3')).toBeInTheDocument()
  })

  it('confirm button is disabled until checkbox is checked', async () => {
    const user = userEvent.setup()
    render(
      <OnetimeSecretModal
        open={true}
        title="Your Passphrase"
        label="Passphrase"
        secret="word1 word2 word3"
        onConfirmed={vi.fn()}
      />
    )
    const button = screen.getByRole('button', { name: /i've saved/i })
    expect(button).toBeDisabled()

    await user.click(screen.getByRole('checkbox'))
    expect(button).toBeEnabled()
  })

  it('calls onConfirmed when confirmed', async () => {
    const user = userEvent.setup()
    const onConfirmed = vi.fn()
    render(
      <OnetimeSecretModal
        open={true}
        title="Your Passphrase"
        label="Passphrase"
        secret="word1 word2 word3"
        onConfirmed={onConfirmed}
      />
    )
    await user.click(screen.getByRole('checkbox'))
    await user.click(screen.getByRole('button', { name: /i've saved/i }))
    await waitFor(() => expect(onConfirmed).toHaveBeenCalledOnce())
  })
})
