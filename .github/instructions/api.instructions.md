---
description: 'API layer patterns and request handling'
applyTo: '**/api/**/*.ts, **/api/**/*.tsx'
---

# API Layer Guidelines

## Request Configuration

Follow the established request pattern with typed responses:

```typescript
// API function example
export const fetchConversations = (): Promise<ConversationList> =>
  request({
    method: 'GET',
    url: '/v1/conversations',
  })

// Type definition
export interface ConversationList {
  conversations: Conversation[]
}
```

## Error Handling

Implement proper async error handling:

```typescript
const handleAuthFlow = async (): Promise<void> => {
  if (isLoading) return

  if (isAuthenticated) {
    try {
      const token = await getAccessTokenSilently()
      storeAccessToken(token)
    } catch {
      removeAccessToken()
      loginWithRedirect({
        appState: { returnTo: window.location.pathname },
      })
    }
    return
  }

  loginWithRedirect({ appState: { returnTo: window.location.pathname } })
}
```

## Type Safety

Use Zod for runtime validation and TypeScript for compile-time safety:

```typescript
// Typed localStorage pattern
export interface LocalStorageSchema {
  access_token: string
  refresh_token: string
  user_id: string
}

export type LocalStorageKey = keyof LocalStorageSchema
```

## Authentication

Follow the established Auth0 integration pattern for token management.