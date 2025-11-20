---
description: 'Vitest and Testing Library patterns for component and unit testing'
applyTo: '**/*.spec.ts, **/*.spec.tsx, **/*.test.ts, **/*.test.tsx'
---

# Testing Guidelines

## Testing Framework

Use Vitest with React Testing Library following established patterns:

```typescript
import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import userEvent from '@testing-library/user-event'

describe('ComponentName', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render correctly with default props', () => {
    render(<ComponentName />)
    expect(screen.getByText('Expected Text')).toBeInTheDocument()
  })

  it('should handle user interactions properly', async () => {
    const user = userEvent.setup()
    render(<ComponentName />)
    
    await user.click(screen.getByRole('button', { name: 'Click me' }))
    
    await waitFor(() => {
      expect(screen.getByText('Result')).toBeInTheDocument()
    })
  })
})
```

## Mocking Patterns

Follow established mocking patterns:

```typescript
// Mock external libraries
vi.mock('@auth0/auth0-react', () => ({
  useAuth0: vi.fn(() => mockUseAuth0),
}))

// Mock internal modules
vi.mock('@/utils/typedLocalStorage')
const mockTypedLocalStorage = vi.mocked(typedLocalStorage)
```

## Test Organization

- Group related tests in `describe` blocks
- Use descriptive test names that explain behavior
- Test user interactions, not implementation details
- Focus on component behavior and user experience