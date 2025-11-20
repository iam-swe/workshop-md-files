---
description: 'Core TypeScript and development standards - Non-negotiable rules'
applyTo: '**/*.ts, **/*.tsx, **/*.js, **/*.jsx'
---

# Core Development Standards

## TypeScript Standards (Non-Negotiable)

- **NEVER use `any` type** - Use `unknown` if type is truly unknown, then narrow with type guards
- **NEVER use type assertions (`as SomeType`)** unless absolutely necessary with clear justification
- **NEVER use `@ts-ignore` or `@ts-expect-error`** without explicit explanation
- **NEVER use `let` or `var`** unless absolutely necessary. Use `const` everywhere and justify any exceptions
- These rules apply to **test code as well as production code**

## Code Structure Rules

- **No nested if/else statements** - Use early returns, guard clauses, or composition
- **Avoid deep nesting** (max 2 levels)
- **Keep functions small** and focused on a single responsibility
- **Prefer flat, readable code** over clever abstractions

## Naming Conventions

- **Files**: PascalCase for components (`Header.tsx`, `LoadingComponent.tsx`)
- **Files**: camelCase for utilities (`typedLocalStorage.ts`, `dateUtils.ts`)
- **Functions**: camelCase, verb-based (`calculateTotal`, `validatePayment`, `fetchConversations`)
- **Types/Interfaces**: PascalCase (`PaymentRequest`, `UserProfile`, `ConversationList`)
- **Constants**: UPPER_SNAKE_CASE for true constants, camelCase for configuration
- **Test files**: `{filename}.spec.{ext}`

## No Comments Policy

Code should be self-documenting through clear naming and structure. Comments indicate unclear code.

## Import Standards

Use `@/` alias for all internal imports consistently:

```typescript
// ✅ Good
import { fetchConversations } from '@/api/conversations/api'
import { typedLocalStorage } from '@/utils/typedLocalStorage'

// ❌ Bad
import { fetchConversations } from '../api/conversations/api'
```