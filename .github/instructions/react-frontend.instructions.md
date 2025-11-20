````instructions
---
description: 'React frontend development standards for Java Spring Boot integration'
applyTo: '**/*.tsx, **/*.jsx, **/package.json, **/*.css, **/*.scss'
---

# React Frontend Standards (Java Spring Boot Integration)

## Core React Standards (Non-Negotiable)

- **NEVER use class components** - Use functional components with hooks
- **NEVER mutate state directly** - Use setState or state management libraries
- **NEVER use inline functions in JSX** that could cause re-renders
- **ALWAYS use TypeScript** for type safety
- **ALWAYS define prop types** with interfaces or types
- **NEVER use index as key** in lists unless items never reorder
- **ALWAYS handle loading and error states** in API calls

## Project Structure

```
src/
├── api/                    # API client and endpoints
│   ├── client.ts          # Axios/Fetch configuration
│   ├── auth.api.ts        # Authentication endpoints
│   └── users.api.ts       # User endpoints
├── components/            # Reusable UI components
│   ├── common/           # Generic components
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   └── Modal.tsx
│   └── layout/           # Layout components
│       ├── Header.tsx
│       ├── Sidebar.tsx
│       └── Footer.tsx
├── pages/                # Page-level components
│   ├── Home.tsx
│   ├── Users.tsx
│   └── Login.tsx
├── hooks/                # Custom React hooks
│   ├── useAuth.ts
│   ├── useUsers.ts
│   └── useDebounce.ts
├── services/             # Business logic layer
│   ├── auth.service.ts
│   └── user.service.ts
├── types/                # TypeScript type definitions
│   ├── api.types.ts
│   ├── user.types.ts
│   └── common.types.ts
├── utils/                # Utility functions
│   ├── validation.ts
│   ├── formatting.ts
│   └── constants.ts
├── context/              # React Context providers
│   ├── AuthContext.tsx
│   └── ThemeContext.tsx
├── styles/               # Global styles
│   ├── globals.css
│   └── theme.ts
└── App.tsx               # Main application component
```

## Naming Conventions

- **Components**: PascalCase (`UserList.tsx`, `LoginForm.tsx`)
- **Hooks**: camelCase with 'use' prefix (`useAuth.ts`, `useFetchUsers.ts`)
- **Utilities**: camelCase (`formatDate.ts`, `validateEmail.ts`)
- **Types/Interfaces**: PascalCase (`UserResponse`, `CreateUserRequest`)
- **Constants**: UPPER_SNAKE_CASE (`API_BASE_URL`, `MAX_FILE_SIZE`)
- **CSS Modules**: camelCase for classes (`.userCard`, `.submitButton`)

## API Integration Standards

```typescript
// ✅ Good - Type-safe API client with error handling
// api/client.ts
import axios, { AxiosError, AxiosInstance } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// api/users.api.ts
export const userApi = {
  getById: async (id: number): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>(`/v1/users/${id}`);
    return response.data;
  },

  create: async (request: CreateUserRequest): Promise<UserResponse> => {
    const response = await apiClient.post<UserResponse>('/v1/users', request);
    return response.data;
  },

  update: async (id: number, request: UpdateUserRequest): Promise<UserResponse> => {
    const response = await apiClient.put<UserResponse>(`/v1/users/${id}`, request);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/v1/users/${id}`);
  },

  list: async (params?: ListUsersParams): Promise<PageResponse<UserResponse>> => {
    const response = await apiClient.get<PageResponse<UserResponse>>('/v1/users', { params });
    return response.data;
  },
};

// ❌ Bad - No types, no error handling
export const getUser = async (id) => {
  const response = await fetch(`/api/users/${id}`);
  return response.json();
};
```

## Type Definitions (Matching Backend DTOs)

```typescript
// ✅ Good - Type definitions matching Spring Boot DTOs
// types/user.types.ts
export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
  MODERATOR = 'MODERATOR',
}

export interface UserResponse {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  phoneNumber?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  email: string;
  name: string;
  role: UserRole;
  phoneNumber?: string;
}

export interface UpdateUserRequest {
  name?: string;
  role?: UserRole;
  phoneNumber?: string;
}

export interface ListUsersParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ApiErrorResponse {
  status: number;
  message: string;
  errors?: Record<string, string>;
  timestamp: string;
}

// ❌ Bad - No types
const user = {
  id: 1,
  name: 'John',
};
```

## Component Standards

```typescript
// ✅ Good - Type-safe component with proper hooks
import { FC, useState, useCallback, useEffect } from 'react';
import { userApi } from '@/api/users.api';
import { UserResponse, CreateUserRequest } from '@/types/user.types';
import { ApiErrorResponse } from '@/types/api.types';

interface UserListProps {
  initialPage?: number;
  pageSize?: number;
  onUserSelect?: (user: UserResponse) => void;
}

export const UserList: FC<UserListProps> = ({ 
  initialPage = 0, 
  pageSize = 10,
  onUserSelect 
}) => {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(initialPage);
  const [totalPages, setTotalPages] = useState(0);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await userApi.list({ page, size: pageSize });
      setUsers(response.content);
      setTotalPages(response.totalPages);
    } catch (err) {
      const error = err as ApiErrorResponse;
      setError(error.message || 'Failed to fetch users');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleUserClick = useCallback((user: UserResponse) => {
    onUserSelect?.(user);
  }, [onUserSelect]);

  if (loading) {
    return <div>Loading users...</div>;
  }

  if (error) {
    return <div className="error">Error: {error}</div>;
  }

  return (
    <div className="user-list">
      {users.map((user) => (
        <div key={user.id} onClick={() => handleUserClick(user)}>
          <h3>{user.name}</h3>
          <p>{user.email}</p>
          <span>{user.role}</span>
        </div>
      ))}
      
      <div className="pagination">
        <button 
          disabled={page === 0}
          onClick={() => setPage(p => p - 1)}
        >
          Previous
        </button>
        <span>Page {page + 1} of {totalPages}</span>
        <button 
          disabled={page >= totalPages - 1}
          onClick={() => setPage(p => p + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
};

// ❌ Bad - No types, inline functions, poor structure
export const UserList = ({ onSelect }) => {
  const [users, setUsers] = useState([]);
  
  useEffect(() => {
    fetch('/api/users').then(r => r.json()).then(setUsers);
  }, []);
  
  return (
    <div>
      {users.map((user, index) => (  // ❌ Using index as key
        <div key={index} onClick={() => onSelect(user)}>  // ❌ Inline function
          {user.name}
        </div>
      ))}
    </div>
  );
};
```

## Form Handling Standards

```typescript
// ✅ Good - Type-safe form with validation
import { FC, useState, FormEvent, ChangeEvent } from 'react';
import { userApi } from '@/api/users.api';
import { CreateUserRequest, UserRole } from '@/types/user.types';

interface CreateUserFormProps {
  onSuccess: () => void;
  onCancel: () => void;
}

interface FormErrors {
  email?: string;
  name?: string;
  role?: string;
}

export const CreateUserForm: FC<CreateUserFormProps> = ({ onSuccess, onCancel }) => {
  const [formData, setFormData] = useState<CreateUserRequest>({
    email: '',
    name: '',
    role: UserRole.USER,
  });
  
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};
    
    if (!formData.email) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Invalid email format';
    }
    
    if (!formData.name) {
      newErrors.name = 'Name is required';
    } else if (formData.name.length < 2 || formData.name.length > 100) {
      newErrors.name = 'Name must be between 2 and 100 characters';
    }
    
    if (!formData.role) {
      newErrors.role = 'Role is required';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    // Clear error for this field
    if (errors[name as keyof FormErrors]) {
      setErrors(prev => ({ ...prev, [name]: undefined }));
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setSubmitting(true);
    setApiError(null);
    
    try {
      await userApi.create(formData);
      onSuccess();
    } catch (err: any) {
      if (err.response?.data?.errors) {
        setErrors(err.response.data.errors);
      } else {
        setApiError(err.response?.data?.message || 'Failed to create user');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="form-group">
        <label htmlFor="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          value={formData.email}
          onChange={handleChange}
          disabled={submitting}
          aria-invalid={!!errors.email}
          aria-describedby={errors.email ? 'email-error' : undefined}
        />
        {errors.email && (
          <span id="email-error" className="error">{errors.email}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="name">Name</label>
        <input
          id="name"
          name="name"
          type="text"
          value={formData.name}
          onChange={handleChange}
          disabled={submitting}
          aria-invalid={!!errors.name}
          aria-describedby={errors.name ? 'name-error' : undefined}
        />
        {errors.name && (
          <span id="name-error" className="error">{errors.name}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="role">Role</label>
        <select
          id="role"
          name="role"
          value={formData.role}
          onChange={handleChange}
          disabled={submitting}
        >
          {Object.values(UserRole).map(role => (
            <option key={role} value={role}>{role}</option>
          ))}
        </select>
        {errors.role && (
          <span className="error">{errors.role}</span>
        )}
      </div>

      {apiError && (
        <div className="alert alert-error">{apiError}</div>
      )}

      <div className="form-actions">
        <button type="button" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" disabled={submitting}>
          {submitting ? 'Creating...' : 'Create User'}
        </button>
      </div>
    </form>
  );
};

// ❌ Bad - No validation, poor error handling
export const CreateUserForm = ({ onSuccess }) => {
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  
  const handleSubmit = async () => {
    await fetch('/api/users', {
      method: 'POST',
      body: JSON.stringify({ email, name }),
    });
    onSuccess();
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <input value={email} onChange={e => setEmail(e.target.value)} />
      <input value={name} onChange={e => setName(e.target.value)} />
      <button>Submit</button>
    </form>
  );
};
```

## Custom Hooks Standards

```typescript
// ✅ Good - Reusable, type-safe custom hook
import { useState, useEffect, useCallback } from 'react';
import { userApi } from '@/api/users.api';
import { UserResponse } from '@/types/user.types';

interface UseUserResult {
  user: UserResponse | null;
  loading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

export const useUser = (userId: number): UseUserResult => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchUser = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      const data = await userApi.getById(userId);
      setUser(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch user');
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  return { user, loading, error, refetch: fetchUser };
};

// Usage
const UserProfile: FC<{ userId: number }> = ({ userId }) => {
  const { user, loading, error, refetch } = useUser(userId);
  
  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!user) return <div>User not found</div>;
  
  return (
    <div>
      <h1>{user.name}</h1>
      <p>{user.email}</p>
      <button onClick={refetch}>Refresh</button>
    </div>
  );
};
```

## State Management Standards

```typescript
// ✅ Good - React Context for auth state
import { createContext, useContext, useState, useCallback, ReactNode, FC } from 'react';
import { authApi } from '@/api/auth.api';
import { UserResponse } from '@/types/user.types';

interface AuthContextValue {
  user: UserResponse | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider: FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const login = useCallback(async (email: string, password: string) => {
    setLoading(true);
    try {
      const response = await authApi.login({ email, password });
      setUser(response.user);
      localStorage.setItem('token', response.token);
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    localStorage.removeItem('token');
  }, []);

  const value: AuthContextValue = {
    user,
    loading,
    login,
    logout,
    isAuthenticated: !!user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

## Performance Optimization

```typescript
// ✅ Good - Optimized component with memo and callbacks
import { FC, memo, useCallback, useMemo } from 'react';
import { UserResponse } from '@/types/user.types';

interface UserCardProps {
  user: UserResponse;
  onEdit: (userId: number) => void;
  onDelete: (userId: number) => void;
}

export const UserCard: FC<UserCardProps> = memo(({ user, onEdit, onDelete }) => {
  const handleEdit = useCallback(() => {
    onEdit(user.id);
  }, [user.id, onEdit]);

  const handleDelete = useCallback(() => {
    onDelete(user.id);
  }, [user.id, onDelete]);

  const formattedDate = useMemo(() => {
    return new Date(user.createdAt).toLocaleDateString();
  }, [user.createdAt]);

  return (
    <div className="user-card">
      <h3>{user.name}</h3>
      <p>{user.email}</p>
      <p>Created: {formattedDate}</p>
      <button onClick={handleEdit}>Edit</button>
      <button onClick={handleDelete}>Delete</button>
    </div>
  );
});

UserCard.displayName = 'UserCard';
```

## Error Boundary

```typescript
// ✅ Good - Error boundary for graceful error handling
import { Component, ReactNode, ErrorInfo } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('Error caught by boundary:', error, errorInfo);
    // Send to error reporting service
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return this.props.fallback || (
        <div className="error-boundary">
          <h1>Something went wrong</h1>
          <p>{this.state.error?.message}</p>
        </div>
      );
    }

    return this.props.children;
  }
}
```

## Testing Standards

```typescript
// ✅ Good - Comprehensive component test
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { UserList } from './UserList';
import { userApi } from '@/api/users.api';

vi.mock('@/api/users.api');

describe('UserList', () => {
  const mockUsers: UserResponse[] = [
    {
      id: 1,
      email: 'user1@example.com',
      name: 'User 1',
      role: UserRole.USER,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render users when API call succeeds', async () => {
    vi.mocked(userApi.list).mockResolvedValue({
      content: mockUsers,
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
    });

    render(<UserList />);

    expect(screen.getByText('Loading users...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('User 1')).toBeInTheDocument();
      expect(screen.getByText('user1@example.com')).toBeInTheDocument();
    });
  });

  it('should display error when API call fails', async () => {
    const errorMessage = 'Failed to fetch users';
    vi.mocked(userApi.list).mockRejectedValue({
      response: { data: { message: errorMessage } },
    });

    render(<UserList />);

    await waitFor(() => {
      expect(screen.getByText(`Error: ${errorMessage}`)).toBeInTheDocument();
    });
  });

  it('should call onUserSelect when user is clicked', async () => {
    const onUserSelect = vi.fn();
    vi.mocked(userApi.list).mockResolvedValue({
      content: mockUsers,
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
    });

    render(<UserList onUserSelect={onUserSelect} />);

    await waitFor(() => {
      expect(screen.getByText('User 1')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('User 1'));
    
    expect(onUserSelect).toHaveBeenCalledWith(mockUsers[0]);
  });
});
```

## Environment Configuration

```typescript
// ✅ Good - Type-safe environment variables
// env.d.ts
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_ENV: 'development' | 'staging' | 'production';
  readonly VITE_ENABLE_MOCK_API: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// config.ts
export const config = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  environment: import.meta.env.VITE_ENV || 'development',
  enableMockApi: import.meta.env.VITE_ENABLE_MOCK_API === 'true',
} as const;
```
````