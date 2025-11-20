---
description: 'React component patterns and state management for Atlas Explore UI'
applyTo: '**/*.tsx, **/*.jsx'
---

# React Development Guidelines

Focused React patterns for Atlas Explore UI using functional components, hooks, and TypeScript.

## Component Patterns

### Functional Components with TypeScript

```typescript
interface LoadingComponentProps {
  readonly message?: string
}

export default function LoadingComponent({
  message = 'Loading...',
}: LoadingComponentProps) {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="text-center">
        <p className="text-lg text-gray-600">{message}</p>
      </div>
    </div>
  )
}
```

### Component Design Principles

- Follow single responsibility principle
- Use descriptive and consistent naming
- Keep components small and focused
- Design for reusability and testability
- Use composition over inheritance

### State Management
- Use `useState` for local component state
- Implement `useReducer` for complex state logic
- Leverage `useContext` for sharing state across component trees
- Consider external state management (Redux Toolkit, Zustand) for complex applications
- Implement proper state normalization and data structures
- Use React Query or SWR for server state management

### Hooks and Effects
- Use `useEffect` with proper dependency arrays to avoid infinite loops
- Implement cleanup functions in effects to prevent memory leaks
- Use `useMemo` and `useCallback` for performance optimization when needed
- Create custom hooks for reusable stateful logic
- Follow the rules of hooks (only call at the top level)
- Use `useRef` for accessing DOM elements and storing mutable values

### Styling
- Use CSS Modules, Styled Components, or modern CSS-in-JS solutions
- Implement responsive design with mobile-first approach
- Follow BEM methodology or similar naming conventions for CSS classes
- Use CSS custom properties (variables) for theming
- Implement consistent spacing, typography, and color systems
- Ensure accessibility with proper ARIA attributes and semantic HTML

### Performance Optimization
- Use `React.memo` for component memoization when appropriate
- Implement code splitting with `React.lazy` and `Suspense`
- Optimize bundle size with tree shaking and dynamic imports
- Use `useMemo` and `useCallback` judiciously to prevent unnecessary re-renders
- Implement virtual scrolling for large lists
- Profile components with React DevTools to identify performance bottlenecks

### Data Fetching
- Use modern data fetching libraries (React Query, Tanstack Query)
- Implement proper loading, error, and success states
- Handle race conditions and request cancellation
- Use optimistic updates for better user experience
- Implement proper caching strategies
- Handle offline scenarios and network errors gracefully

### Error Handling
- Implement Error Boundaries for component-level error handling
- Use proper error states in data fetching
- Implement fallback UI for error scenarios
- Log errors appropriately for debugging
- Handle async errors in effects and event handlers
- Provide meaningful error messages to users

### Forms and Validation
- Use controlled components for form inputs
- Implement proper form validation with libraries like Formik, React Hook Form
- Handle form submission and error states appropriately
- Implement accessibility features for forms (labels, ARIA attributes)
- Use debounced validation for better user experience
- Handle file uploads and complex form scenarios

### Routing
- Use TanStack Router for type-safe client-side routing with full TypeScript inference
- Implement file-based routing with `createFileRoute()` for automatic route generation
- Use nested layout routes with `Outlet` component for shared UI components
- Handle route parameters and search params with full type safety
- Implement data loading with `loader` functions and proper loading/error states
- Use `useNavigate()` hook for imperative navigation and `Link` component for declarative navigation
- Implement route protection with `beforeLoad` guards and authentication checks
- Leverage pathless layout routes (prefixed with `_`) for UI organization without URL changes
- Use route trees for complex nested routing structures with proper parent-child relationships

### Testing
- Use Vitest as the test runner with React Testing Library for component testing
- Test component behavior and user interactions, not implementation details
- Use `render`, `screen`, and `waitFor` from `@testing-library/react` with jsdom environment
- Mock external dependencies with `vi.mock()` for clean, isolated tests
- Use `@testing-library/user-event` for realistic user interaction simulation
- Test accessibility features using proper ARIA queries and semantic HTML testing
- Implement integration tests for complex component interactions and data flow
- Use descriptive test names that focus on user-facing behavior and expected outcomes
- Leverage established test utilities: `ComponentTestWrapper`, `RouteTestRenderer`, and `QueryClientWrapper`

### Security
- Sanitize user inputs to prevent XSS attacks
- Validate and escape data before rendering
- Use HTTPS for all external API calls
- Implement proper authentication and authorization patterns
- Avoid storing sensitive data in localStorage or sessionStorage
- Use Content Security Policy (CSP) headers

### Accessibility
- Use semantic HTML elements appropriately
- Implement proper ARIA attributes and roles
- Ensure keyboard navigation works for all interactive elements
- Provide alt text for images and descriptive text for icons
- Implement proper color contrast ratios
- Test with screen readers and accessibility tools

## Implementation Process
1. Plan component architecture and data flow
2. Set up project structure with proper folder organization
3. Define TypeScript interfaces and types
4. Implement core components with proper styling
5. Add state management and data fetching logic
6. Implement routing and navigation
7. Add form handling and validation
8. Implement error handling and loading states
9. Add testing coverage for components and functionality
10. Optimize performance and bundle size
11. Ensure accessibility compliance
12. Add documentation and code comments

## Additional Guidelines
- Follow React's naming conventions (PascalCase for components, camelCase for functions)
- Use meaningful commit messages and maintain clean git history
- Implement proper code splitting and lazy loading strategies
- Document complex components and custom hooks with JSDoc
- Use ESLint and Prettier for consistent code formatting
- Keep dependencies up to date and audit for security vulnerabilities
- Implement proper environment configuration for different deployment stages
- Use React Developer Tools for debugging and performance analysis

## Common Patterns
- Higher-Order Components (HOCs) for cross-cutting concerns
- Render props pattern for component composition
- Compound components for related functionality
- Provider pattern for context-based state sharing
- Container/Presentational component separation
- Custom hooks for reusable logic extraction