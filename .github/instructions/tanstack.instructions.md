---
description: 'TanStack Router and Query patterns for routing and data fetching'
applyTo: '**/routes/**/*.tsx, **/routes/**/*.ts, **/*query*.ts, **/*api*.ts'
---

# TanStack Router & Query Guidelines

Specific patterns for TanStack Router file-based routing and TanStack Query data fetching in Atlas Explore UI.

## Component Patterns

Use function components with proper TypeScript interfaces:

```typescript
interface ButtonProps {
  children: React.ReactNode;
  onClick: () => void;
  variant?: 'primary' | 'secondary';
}

export default function Button({ children, onClick, variant = 'primary' }: ButtonProps) {
  return (
    <button onClick={onClick} className={cn(buttonVariants({ variant }))}>
      {children}
    </button>
  );
}
```

## Data Fetching

Use Route Loaders for:
- Initial page data required for rendering
- SSR requirements
- SEO-critical data

Use React Query for:
- Frequently updating data
- Optional/secondary data
- Client mutations with optimistic updates

```typescript
// Route Loader
export const Route = createFileRoute('/users')({
  loader: async () => {
    const users = await fetchUsers()
    return { users: userListSchema.parse(users) }
  },
  component: UserList,
})

// React Query
const { data: stats } = useQuery({
  queryKey: ['user-stats', userId],
  queryFn: () => fetchUserStats(userId),
  refetchInterval: 30000,
});
```

## Routes

Structure routes in `src/routes/` with file-based routing. Always include error and pending boundaries:

```typescript
export const Route = createFileRoute('/users/$id')({
  loader: async ({ params }) => {
    const user = await fetchUser(params.id);
    return { user: userSchema.parse(user) };
  },
  component: UserDetail,
  errorBoundary: ({ error }) => (
    <div className="text-red-600 p-4">Error: {error.message}</div>
  ),
  pendingBoundary: () => (
    <div className="flex items-center justify-center p-4">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
    </div>
  ),
});
```

## Import Standards

Use `@/` alias for all internal imports:

```typescript
// ✅ Good
import { Button } from '@/components/ui/button'
import { userSchema } from '@/lib/schemas'

// ❌ Bad
import { Button } from '../components/ui/button'
```

## Common Patterns

- Use route loaders for initial data, React Query for updates
- Include error/pending boundaries on all routes
- Use `@/` imports consistently