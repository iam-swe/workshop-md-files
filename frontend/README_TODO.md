# To-Do List Frontend

React frontend for the file-based to-do list application.

## Tech Stack

- **React** - UI library
- **TypeScript** - Type safety
- **TanStack React Query** - Server state management
- **Axios** - HTTP client
- **React Hook Form** - Form validation and management

## Prerequisites

- Node.js 16+ and npm
- Backend server running on `http://localhost:8080`

## Installation

```bash
npm install
```

## Running the Application

### Development Mode
```bash
npm start
```

The application will open at [http://localhost:3000](http://localhost:3000).

### Production Build
```bash
npm run build
```

## Running Tests

```bash
npm test
```

## Environment Variables

Create `.env.development` for local development:
```
REACT_APP_API_BASE_URL=http://localhost:8080
```

For production, set:
```
REACT_APP_API_BASE_URL=https://api.yourdomain.com
```

## Features

- ✅ Add new tasks
- ✅ View all tasks sorted by creation date
- ✅ Toggle task completion status
- ✅ Delete tasks
- ✅ Real-time updates with React Query
- ✅ Loading and error states
- ✅ Responsive design
- ✅ Form validation

## Project Structure

```
src/
├── api/                 # API client and endpoints
│   ├── client.ts       # Axios configuration
│   └── tasks.api.ts    # Task API functions
├── components/          # React components
│   ├── TaskForm.tsx    # Form to add tasks
│   ├── TaskItem.tsx    # Individual task display
│   ├── TaskList.tsx    # Task list container
│   └── TodoApp.tsx     # Main app component
├── hooks/              # Custom React hooks
│   ├── useTasks.ts     # Fetch tasks
│   ├── useCreateTask.ts # Create task mutation
│   ├── useToggleTask.ts # Toggle task mutation
│   └── useDeleteTask.ts # Delete task mutation
├── types/              # TypeScript definitions
│   └── task.types.ts   # Task interfaces
├── __tests__/          # Component tests
├── App.js              # App root with QueryClient
└── index.js            # Entry point
```

## Component Documentation

### TaskForm
- Controlled form with validation
- Validates description (required, max 1000 chars)
- Clears input on successful submission
- Shows loading state during creation

### TaskItem
- Displays task with checkbox and delete button
- Shows creation and completion timestamps
- Formats dates as "X hours/days ago"
- Accessible with ARIA labels

### TaskList
- Fetches and displays all tasks
- Shows loading skeleton during fetch
- Handles error states with retry button
- Shows empty state when no tasks exist

## Testing

Tests are written using:
- **React Testing Library** - Component testing
- **Jest** - Test runner
- **MSW (Mock Service Worker)** - API mocking

Run tests with:
```bash
npm test
```

## API Integration

The frontend communicates with the backend REST API:

- `GET /api/v1/tasks` - Fetch all tasks
- `POST /api/v1/tasks` - Create new task
- `PATCH /api/v1/tasks/:id/toggle` - Toggle completion
- `DELETE /api/v1/tasks/:id` - Delete task

## Error Handling

- Network errors show user-friendly messages
- Form validation errors displayed inline
- API errors caught and displayed with retry option
- Optimistic updates with rollback on failure
