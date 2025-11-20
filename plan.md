# Implementation Plan: File-Based To-Do List Application

## Executive Summary

Building a full-stack file-based to-do list application using **Java Spring Boot** backend with **React TypeScript** frontend. The application will follow the standard Controller-Service-Repository layered architecture but replace the JPA/database layer with a custom file-based repository that persists tasks to a JSON file. This approach maintains adherence to project instruction file patterns while meeting the story's file-based persistence requirement.

**Core Features**:
- Add new tasks with descriptions
- Display all tasks sorted by creation date
- Toggle task completion status
- Delete tasks
- Persist all data to a JSON file with atomic write operations
- Type-safe REST API with full error handling

## Selected Approach: Spring Boot REST API + React Frontend (File-Based Storage)

**Rationale**: This approach provides the best balance between:
- **Instruction File Compliance**: Follows Spring Boot and React patterns from `.github/instructions/`
- **Production Quality**: Proper error handling, validation, testing, and scalability
- **Migration Path**: Easy to swap file repository for JPA repository when database is needed
- **Full-Stack Completeness**: Delivers complete user-facing application meeting all acceptance criteria

## Architecture Overview

- **Pattern**: Three-tier layered architecture (Controller → Service → Repository → File)
- **Backend Strategy**: RESTful API with file-based repository implementing atomic writes and thread-safe operations
- **Frontend Strategy**: React Query for server state management with type-safe API client
- **Integration**: JSON DTOs with TypeScript interfaces ensuring type safety across the stack

## Files to Modify/Create

### Backend Files

#### Core Domain & Storage
- `src/main/java/com/company/todo/model/Task.java`
  - Plain Java object (not JPA entity) representing task domain model
  - Fields: `UUID id`, `String description`, `boolean completed`, `LocalDateTime createdAt`, `LocalDateTime completedAt`
  - Uses Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` for clean code
  - Includes Bean Validation annotations (`@NotBlank`, `@Size`)

#### Repository Layer
- `src/main/java/com/company/todo/repository/TaskRepository.java`
  - Interface defining repository contract (similar to JpaRepository but custom)
  - Methods: `List<Task> findAll()`, `Optional<Task> findById(UUID)`, `Task save(Task)`, `void deleteById(UUID)`, `boolean existsById(UUID)`
  - This abstraction allows future database migration

- `src/main/java/com/company/todo/repository/impl/TaskFileRepositoryImpl.java`
  - Implementation using Java NIO file operations and Jackson for JSON
  - Thread-safe operations using `ReentrantReadWriteLock`
  - Atomic write pattern: write to `.tmp` file, then `Files.move()` with `ATOMIC_MOVE` option
  - File format: `{ "version": "1.0", "lastModified": "timestamp", "tasks": [...] }`
  - Automatic backup creation before writes (copy to `.json.backup`)
  - File corruption detection on load with recovery options
  - Configurable file path from `application.yml`

#### Service Layer
- `src/main/java/com/company/todo/service/TaskService.java`
  - Interface defining business operations
  
- `src/main/java/com/company/todo/service/impl/TaskServiceImpl.java`
  - Business logic: create, read, toggle completion, delete
  - UUID generation for new tasks
  - Timestamp management (`createdAt` on creation, `completedAt` on toggle)
  - Sorting logic: tasks sorted by `createdAt` descending (newest first)
  - Validation: trim description, check for empty/whitespace
  - Exception throwing for not found scenarios

#### DTO Layer
- `src/main/java/com/company/todo/dto/request/CreateTaskRequest.java`
  - Single field: `@NotBlank @Size(min=1, max=1000) String description`
  - Lombok `@Data` and `@Builder`

- `src/main/java/com/company/todo/dto/response/TaskResponse.java`
  - Maps all fields from `Task` model
  - Fields: `String id`, `String description`, `boolean completed`, `String createdAt`, `String completedAt`
  - Date fields formatted as ISO-8601 strings

- `src/main/java/com/company/todo/dto/response/ErrorResponse.java`
  - Standardized error structure: `int status`, `String message`, `String timestamp`, `String path`

#### Controller Layer
- `src/main/java/com/company/todo/controller/TaskController.java`
  - `@RestController` with `@RequestMapping("/api/v1/tasks")`
  - Constructor injection with `@RequiredArgsConstructor`
  - Endpoints:
    - `GET /api/v1/tasks` → Returns `List<TaskResponse>` (200 OK)
    - `POST /api/v1/tasks` → Accepts `@Valid CreateTaskRequest`, returns `TaskResponse` (201 CREATED)
    - `PATCH /api/v1/tasks/{id}/toggle` → Returns `TaskResponse` (200 OK)
    - `DELETE /api/v1/tasks/{id}` → Returns void (204 NO_CONTENT)
  - Each method validates inputs and returns appropriate `ResponseEntity` with status codes

#### Exception Handling
- `src/main/java/com/company/todo/exception/TaskNotFoundException.java`
  - Extends `RuntimeException`, thrown when task ID not found

- `src/main/java/com/company/todo/exception/FileStorageException.java`
  - Thrown for file I/O errors (read failures, write failures, corruption)

- `src/main/java/com/company/todo/exception/InvalidTaskException.java`
  - Thrown for validation failures beyond Bean Validation

- `src/main/java/com/company/todo/exception/GlobalExceptionHandler.java`
  - `@RestControllerAdvice` with `@ExceptionHandler` methods
  - Maps exceptions to appropriate HTTP status codes and `ErrorResponse` DTOs
  - Logs errors with appropriate severity levels

#### Configuration
- `src/main/java/com/company/todo/config/FileStorageConfig.java`
  - `@Configuration` class for file storage setup
  - Reads file path from properties
  - Creates directory if not exists
  - Validates write permissions on startup

- `src/main/java/com/company/todo/config/CorsConfig.java`
  - CORS configuration for local development (allow `localhost:5173` for Vite)
  - Production CORS settings (restrict to actual frontend domain)

- `src/main/resources/application.yml`
  - File storage path: `todo.file.path: ${user.home}/.todo/tasks.json`
  - CORS allowed origins
  - Logging levels for file operations

- `src/main/resources/application-dev.yml`
  - Development-specific settings (verbose logging, permissive CORS)

- `src/main/resources/application-prod.yml`
  - Production settings (error-level logging, restricted CORS)

#### Testing Files
- `src/test/java/com/company/todo/service/TaskServiceTest.java`
  - Unit tests using `@ExtendWith(MockitoExtension.class)`
  - Mock `TaskRepository` with Mockito
  - Test create, toggle, delete operations
  - Test exception scenarios (not found, validation failures)
  - Follow AAA pattern: Arrange, Act, Assert
  - Descriptive test names: `createTask_WithValidDescription_ShouldReturnCreatedTask()`

- `src/test/java/com/company/todo/repository/TaskFileRepositoryTest.java`
  - Integration tests using `@TempDir` for isolated file operations
  - Test atomic writes, concurrent access, corruption recovery
  - Test backup creation and restoration
  - Verify file format integrity

- `src/test/java/com/company/todo/controller/TaskControllerTest.java`
  - Controller tests with `@WebMvcTest(TaskController.class)`
  - Use MockMvc to test endpoints
  - Mock service layer
  - Verify HTTP status codes, response bodies, error handling

#### Build Configuration
- `pom.xml` (if Maven) or `build.gradle` (if Gradle)
  - Dependencies: Spring Boot Web, Jackson, Lombok, Validation
  - Test dependencies: JUnit 5, Mockito, Spring Boot Test
  - Build plugins for running tests, code coverage (JaCoCo)

### Frontend Files

#### Type Definitions
- `src/types/task.types.ts`
  - `Task` interface matching `TaskResponse`: `{ id: string, description: string, completed: boolean, createdAt: string, completedAt: string | null }`
  - `CreateTaskRequest` interface: `{ description: string }`
  - `ApiError` interface for error responses

#### API Client Layer
- `src/api/client.ts`
  - Axios instance configured with base URL from environment variable
  - Base URL: `import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'`
  - Request interceptor for auth tokens (if needed later)
  - Response interceptor for global error handling
  - Timeout configuration (10 seconds)

- `src/api/tasks.api.ts`
  - API endpoint functions with full TypeScript typing
  - `getTasks(): Promise<Task[]>` - GET `/api/v1/tasks`
  - `createTask(request: CreateTaskRequest): Promise<Task>` - POST `/api/v1/tasks`
  - `toggleTask(id: string): Promise<Task>` - PATCH `/api/v1/tasks/{id}/toggle`
  - `deleteTask(id: string): Promise<void>` - DELETE `/api/v1/tasks/{id}`
  - Error handling with try-catch and typed error responses

#### Custom Hooks
- `src/hooks/useTasks.ts`
  - React Query hook for fetching tasks: `useQuery(['tasks'], getTasks)`
  - Auto-refetch on window focus
  - Stale time configuration
  - Loading and error state handling

- `src/hooks/useCreateTask.ts`
  - React Query mutation hook: `useMutation(createTask)`
  - Optimistic updates for instant UI feedback
  - Query invalidation on success to refetch task list
  - Error handling with user-friendly messages

- `src/hooks/useToggleTask.ts`
  - Mutation hook for toggling completion
  - Optimistic UI update (toggle locally, then sync)
  - Rollback on error

- `src/hooks/useDeleteTask.ts`
  - Mutation hook for deleting tasks
  - Optimistic removal from list
  - Confirmation dialog before deletion (optional)

#### Components
- `src/components/TaskList.tsx`
  - Displays list of tasks using `useTasks` hook
  - Handles loading state (skeleton loaders)
  - Handles error state (error message with retry button)
  - Empty state when no tasks ("No tasks yet, add one above!")
  - Maps over tasks and renders `TaskItem` components
  - Props: none (fetches own data)

- `src/components/TaskItem.tsx`
  - Individual task display with checkbox and delete button
  - Props interface: `{ task: Task, onToggle: (id: string) => void, onDelete: (id: string) => void }`
  - Checkbox for completion status (calls `useToggleTask`)
  - Delete button with icon (calls `useDeleteTask`)
  - Displays creation date formatted (e.g., "Created 2 hours ago")
  - Accessibility: Proper ARIA labels, keyboard navigation support

- `src/components/TaskForm.tsx`
  - Form to add new tasks using React Hook Form
  - Controlled input field for task description
  - Validation: required field, max length 1000 characters
  - Submit handler using `useCreateTask` hook
  - Loading state on submit button (disabled during creation)
  - Error display for validation or API errors
  - Auto-clear input on successful submission
  - Focus management (focus input after submission)

- `src/pages/TodoApp.tsx`
  - Main page component composing `TaskForm` and `TaskList`
  - Layout structure with header, main content area
  - Optional: Filter buttons (All, Active, Completed) - can be deferred
  - Title and description

#### Application Setup
- `src/App.tsx`
  - Root component with `QueryClientProvider` from React Query
  - Query client configuration (default options, retry logic)
  - Error boundary for catching component errors
  - Routing setup if needed (single page for now)

- `src/main.tsx` or `src/index.tsx`
  - Entry point rendering `App` component
  - StrictMode wrapper for development

#### Testing Files
- `src/__tests__/components/TaskList.test.tsx`
  - Component tests using React Testing Library
  - Mock `useTasks` hook with MSW or manual mocks
  - Test loading state rendering
  - Test task list rendering
  - Test empty state
  - Test error state with retry button

- `src/__tests__/components/TaskForm.test.tsx`
  - Test form submission with valid input
  - Test validation errors (empty input, too long)
  - Test loading state during submission
  - Test successful submission (input clears)
  - User interaction tests (typing, clicking submit)

- `src/__tests__/hooks/useTasks.test.ts`
  - Hook tests with `@testing-library/react-hooks`
  - Mock API responses with MSW
  - Test successful data fetching
  - Test error handling
  - Test refetch behavior

#### Configuration Files
- `package.json`
  - Dependencies: `react`, `react-dom`, `typescript`, `axios`, `@tanstack/react-query`, `react-hook-form`
  - Dev dependencies: `vite`, `vitest`, `@testing-library/react`, `@testing-library/react-hooks`, `msw`
  - Scripts: `dev`, `build`, `test`, `preview`

- `.env.development`
  - `VITE_API_BASE_URL=http://localhost:8080`

- `.env.production`
  - `VITE_API_BASE_URL=https://api.yourdomain.com` (configure for actual deployment)

- `vite.config.ts`
  - Vite configuration for React, TypeScript
  - Test configuration for Vitest
  - Proxy configuration for local API (optional)

## Key Implementation Ideas

### Backend Core Concepts

#### File Repository Pattern
- Repository will maintain an in-memory cache of tasks loaded from file
- On startup, load tasks from file into memory (or initialize empty if file doesn't exist)
- All read operations (`findAll`, `findById`) operate on in-memory data for speed
- All write operations (`save`, `delete`) update in-memory data AND persist to file
- File write strategy:
  1. Serialize current task list to JSON string
  2. Write JSON to temporary file (`tasks.json.tmp`)
  3. Atomically move temp file to actual file (`Files.move` with `ATOMIC_MOVE`)
  4. This prevents corruption if process crashes during write

#### Thread Safety
- Use `ReentrantReadWriteLock` to allow multiple concurrent reads but exclusive writes
- Read operations acquire read lock
- Write operations acquire write lock (blocks all other operations)
- Ensures data consistency under concurrent access

#### Error Recovery
- On file load, catch JSON parsing exceptions
- If file is corrupted:
  1. Log error with full stack trace
  2. Check for `.backup` file
  3. If backup exists, restore from backup
  4. If no backup, initialize empty task list
  5. Notify caller (service layer) of recovery action taken

#### Validation Strategy
- Bean Validation on DTOs (`@Valid` in controller)
- Additional business validation in service layer (trim whitespace, check for duplicate IDs)
- Service layer throws domain-specific exceptions
- Global exception handler maps to HTTP responses

### Frontend Core Concepts

#### React Query State Management
- React Query manages server state (task data from API)
- Query keys: `['tasks']` for task list
- Mutations invalidate queries to trigger refetch
- Optimistic updates for instant UI feedback:
  - When toggling task, update local cache immediately
  - If API call fails, rollback to previous state
  - Show success/error toast notifications

#### Form Handling Pattern
- React Hook Form reduces boilerplate for form state
- Schema validation with inline rules or Zod schema
- Error messages displayed below input field
- Submit handler is async function calling mutation hook
- Disable submit button during submission (prevent double-submit)

#### Component Composition
- `TodoApp` is container component (stateful, data fetching)
- `TaskList` and `TaskForm` are presentation components (receive data via props or hooks)
- `TaskItem` is pure presentation (receives task and callbacks)
- This separation makes components easier to test and reuse

#### Error Handling UX
- Loading states: Show skeleton loaders or spinners
- Error states: Display user-friendly message with retry action
- Success feedback: Brief toast notification or visual confirmation
- Network errors: Distinguish between 4xx (client error) and 5xx (server error)

## Integration Points

### API Contracts
- **Endpoint Design**: RESTful with resource-based URLs (`/tasks`, `/tasks/{id}`)
- **DTO Structure**: Backend DTOs and Frontend TypeScript interfaces must match exactly
- **Date Format**: ISO-8601 strings for dates (`2025-11-20T10:30:00Z`)
- **Error Format**: Consistent `ErrorResponse` structure for all errors

### CORS Configuration
- Backend configures CORS to allow frontend origin
- Development: Allow `http://localhost:5173` (Vite default)
- Production: Restrict to actual frontend domain
- Allow methods: GET, POST, PATCH, DELETE
- Allow headers: Content-Type, Authorization (for future auth)

### Data Flow
1. **Read Flow**: Frontend → React Query → Axios → Backend Controller → Service → Repository → In-Memory Cache → Response
2. **Write Flow**: Frontend → Mutation → Axios → Backend Controller → Service → Repository → File Write → Response → Query Invalidation

### Error Propagation
- Backend exceptions caught by `@RestControllerAdvice`
- Mapped to HTTP status codes (400, 404, 500)
- Returned as `ErrorResponse` JSON
- Frontend Axios interceptor catches errors
- Error displayed to user with retry option

## Testing Strategy

### Backend Testing Approach

#### Unit Tests (Service Layer)
- Mock repository with Mockito
- Test each service method in isolation
- Test happy paths and error scenarios
- Example tests:
  - `createTask_WithValidDescription_ShouldGenerateIdAndTimestamp()`
  - `toggleTask_WhenTaskNotFound_ShouldThrowNotFoundException()`
  - `deleteTask_WhenTaskExists_ShouldRemoveFromRepository()`
- Verify method calls on mocked dependencies
- Assert correct return values and exceptions

#### Integration Tests (Repository Layer)
- Use `@TempDir` to create temporary test directory
- Test actual file operations without mocking
- Verify JSON file content after writes
- Test concurrent access with multiple threads
- Test corruption recovery scenarios
- Example tests:
  - `save_WithNewTask_ShouldWriteToFileAndCreateBackup()`
  - `findAll_WhenFileCorrupted_ShouldRestoreFromBackup()`

#### Controller Tests
- Use `@WebMvcTest` with MockMvc
- Mock service layer
- Test HTTP endpoints, status codes, response bodies
- Verify request validation (invalid DTOs return 400)
- Example tests:
  - `createTask_WithValidRequest_ShouldReturn201Created()`
  - `getTask_WhenNotFound_ShouldReturn404()`

### Frontend Testing Approach

#### Component Tests
- Use React Testing Library
- Test user interactions (click, type, submit)
- Test rendering based on props and state
- Mock API calls with MSW (Mock Service Worker)
- Example tests:
  - "TaskForm should clear input after successful submission"
  - "TaskList should show loading skeleton while fetching"
  - "TaskItem should call onToggle when checkbox clicked"

#### Hook Tests
- Test custom hooks with `renderHook` from testing library
- Mock API responses
- Verify hook returns expected data and states
- Test error handling and retries

#### Integration Tests
- Test complete user flows (add task → toggle → delete)
- Use MSW to mock API at network level
- Verify UI updates correctly after API responses

## Success Criteria

- ✅ **AC1: Add New Task** - User can submit form, task appears in list, persisted to file
- ✅ **AC2: Display All Tasks** - Tasks load on app start, sorted newest first, show description and status
- ✅ **AC3: Mark Complete** - Clicking checkbox toggles status, persisted immediately, completion timestamp recorded
- ✅ **AC4: Data Persistence** - Close app, reopen, all tasks and states retained
- ✅ **AC5: File Format Integrity** - File contains valid JSON with required fields, can be inspected manually
- ✅ **Performance** - Task operations complete in <100ms for typical use (<1000 tasks)
- ✅ **Error Handling** - File errors show user-friendly messages, app doesn't crash
- ✅ **Testing** - 80%+ code coverage on backend, critical UI flows tested on frontend
- ✅ **Type Safety** - No TypeScript `any` types, all API contracts strictly typed
- ✅ **Instruction File Compliance** - Follows all patterns from Java and React instruction files

## Implementation Phases

### Phase 1: Backend Foundation (Day 1)
1. Create Spring Boot project structure
2. Implement `Task` model and DTOs
3. Implement `TaskFileRepository` with file operations
4. Write repository tests
5. Implement `TaskService` with business logic
6. Write service tests

### Phase 2: Backend API (Day 1-2)
1. Implement `TaskController` with endpoints
2. Add global exception handling
3. Configure CORS
4. Write controller tests
5. Manual API testing with Postman/curl

### Phase 3: Frontend Setup (Day 2)
1. Create React TypeScript project with Vite
2. Define TypeScript interfaces
3. Set up Axios API client
4. Configure React Query

### Phase 4: Frontend UI (Day 2-3)
1. Build `TaskForm` component with validation
2. Build `TaskItem` component
3. Build `TaskList` component with loading/error states
4. Compose `TodoApp` page
5. Style components (CSS/Tailwind)

### Phase 5: Integration & Testing (Day 3)
1. Connect frontend to backend API
2. Test full user flows
3. Write frontend component tests
4. Fix bugs and edge cases
5. Performance testing (1000 tasks)

### Phase 6: Documentation & Deployment (Optional)
1. Update README with setup instructions
2. Add API documentation (Swagger)
3. Configure production environment variables
4. Deploy backend and frontend

## Notes and Considerations

### Critical Implementation Details

**File Path Configuration**: Use `${user.home}/.todo/tasks.json` as default, but allow override via environment variable for testing and different deployments.

**UUID vs. Auto-Increment ID**: Using UUID for task IDs avoids collision issues and makes IDs globally unique. Frontend treats as string, backend as `UUID` type.

**Atomic Writes**: The write-to-temp-then-rename pattern is crucial for preventing file corruption. Java's `Files.move()` with `StandardCopyOption.ATOMIC_MOVE` ensures atomicity on most filesystems.

**Completion Timestamp**: When toggling task to completed, set `completedAt` timestamp. When toggling back to incomplete, clear `completedAt` (set to null). This allows future features like "completed tasks in last week".

**Sorting Strategy**: Tasks sorted by `createdAt` descending (newest first). This happens in service layer before returning to controller. Could be enhanced with configurable sorting later.

**Validation Layers**: Bean Validation on DTOs catches basic issues (blank description). Service layer adds business validation (trim whitespace, check for special characters if needed). Both layers important for defense in depth.

### Potential Challenges

**File Locking on Windows**: Windows file locking is more aggressive than Unix. Test thoroughly on Windows if targeting that platform. May need additional retry logic.

**Large File Performance**: Current design loads entire file into memory. Works well for <1000 tasks. If scaling beyond that, consider streaming JSON parsing or pagination.

**Concurrent Writes**: While `ReadWriteLock` prevents data races in JVM, multiple JVM instances writing to same file would cause issues. Document as single-instance limitation or implement file-level locking.

**Error Messages**: Balance between helpful (for developers) and secure (don't leak file paths to users). Production error messages should be generic, detailed errors only in logs.

### Future Enhancements

**Edit Task Description**: Add `PUT /api/v1/tasks/{id}` endpoint and edit UI. Service layer updates description field, file repository persists.

**Task Categories/Tags**: Extend `Task` model with tags field (`List<String>`), update DTOs, add filtering in frontend.

**Search/Filter**: Add query parameters to `GET /tasks` endpoint for filtering by completion status or description search.

**Database Migration**: When ready, create JPA entity from `Task` model, implement `JpaRepository`, swap in service layer. File repository can be used for data migration.

**Backup Management**: Automatic backup rotation (keep last 5 backups), scheduled backups, export/import functionality.

---

This plan provides the architectural foundation and key implementation ideas for building the file-based to-do list application. The actual implementation will involve writing code based on these concepts, following the patterns and standards defined in the project's instruction files.

**Next Steps**: Begin Phase 1 (Backend Foundation) by setting up the Spring Boot project and implementing the file repository layer.
