import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import type { RenderOptions } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

function makeTestQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
}

interface WrapperProps {
  children: React.ReactNode
  initialEntries?: string[]
}

function AllProviders({ children, initialEntries = ['/'] }: WrapperProps) {
  const client = makeTestQueryClient()
  return (
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
    </QueryClientProvider>
  )
}

const customRender = (ui: ReactElement, options?: Omit<RenderOptions, 'wrapper'> & { initialEntries?: string[] }) => {
  const { initialEntries, ...rest } = options ?? {}
  return render(ui, {
    wrapper: ({ children }) => <AllProviders initialEntries={initialEntries}>{children}</AllProviders>,
    ...rest,
  })
}

export * from '@testing-library/react'
export { customRender as render }
