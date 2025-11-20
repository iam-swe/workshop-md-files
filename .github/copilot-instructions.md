---
description: 'Coding conventions and guidelines for Java Spring Boot + React applications'
applyTo: '**/*.java, **/*.ts, **/*.tsx, **/*.jsx'
---

# Coding Conventions for Java Spring Boot + React Applications

> **Note**: This file provides high-level guidelines. For detailed, comprehensive standards, refer to the instruction files below.

## Instruction Files

### Backend (Java Spring Boot)
| File | Applies To | Description |
| ------- | --------- | ----------- |
| `.github/instructions/java-backend.instructions.md` | **/*.java, **/pom.xml, **/build.gradle | Core Java Spring Boot backend standards, REST API patterns, service layer, repository patterns, DTOs, and exception handling |
| `.github/instructions/java-testing.instructions.md` | **/*Test.java, **/*IT.java, **/test/**/*.java | Java testing standards - Unit tests, integration tests, mocking, test data builders, and coverage |
| `.github/instructions/java-security.instructions.md` | **/security/**/*.java, **/config/Security*.java | Security standards - JWT authentication, Spring Security, rate limiting, input validation |
| `.github/instructions/java-database.instructions.md` | **/repository/**/*.java, **/entity/**/*.java, **/migration/**/*.sql | Database and JPA standards - Entity design, repository patterns, query optimization, transactions |
| `.github/instructions/java-dev-environment.instructions.md` | **/application*.yml, **/config/**/*.java | Development environment - Configuration management, profiles, logging, monitoring, caching |

### Frontend (React + TypeScript)
| File | Applies To | Description |
| ------- | --------- | ----------- |
| `.github/instructions/react-frontend.instructions.md` | **/*.tsx, **/*.jsx, **/package.json | React frontend standards with Java Spring Boot integration, API client patterns, type-safe forms, hooks, and testing |
| `.github/instructions/api.instructions.md` | **/api/**/*.ts, **/api/**/*.tsx | API layer patterns and request handling |
| `.github/instructions/core.instructions.md` | **/*.ts, **/*.tsx, **/*.js, **/*.jsx | Core TypeScript and development standards - Non-negotiable rules |
| `.github/instructions/react.instructions.md` | **/*.tsx, **/*.jsx | React component patterns and state management |
| `.github/instructions/tanstack.instructions.md` | **/routes/**/*.tsx, **/routes/**/*.ts, **/*query*.ts | TanStack Router and Query patterns |
| `.github/instructions/testing.instructions.md` | **/*.spec.ts, **/*.spec.tsx, **/*.test.ts, **/*.test.tsx | Vitest and Testing Library patterns |

## Java Spring Boot Backend Instructions

- Write clear and concise comments only when necessary to explain complex logic or business rules.
- Ensure methods have descriptive names and follow Java naming conventions.
- **Do not add Javadoc for every method** - only add them for public APIs and complex business logic.
- Use proper Java generics for type safety (e.g., `List<String>`, `Map<String, Object>`, `Optional<T>`).
- Break down complex methods into smaller, focused methods with single responsibilities.
- Use `java.nio.file.Path` for file system operations instead of `java.io.File`.
- Prefer immutable objects and final variables where possible.
- Use Spring's dependency injection (`@Autowired`, `@Service`, `@Repository`) consistently.
- Follow RESTful API design principles for controller endpoints.

## React Frontend Instructions

- Write clear and concise comments only when necessary to explain complex logic or business rules.
- Use functional components with hooks instead of class components.
- Follow React naming conventions: PascalCase for components, camelCase for variables/functions.
- Use TypeScript for type safety across the frontend application.
- Break down complex components into smaller, reusable components with single responsibilities.
- Use proper prop types and interfaces for component props.
- Implement proper state management (React Context, Redux, or Zustand as appropriate).
- Follow React best practices for performance optimization (memo, useMemo, useCallback).

## Application Architecture Guidelines

- Follow layered architecture: Controller → Service → Repository for backend.
- Use dependency injection patterns for external services and APIs.
- Implement proper error handling with custom exceptions where appropriate.
- Use structured logging with SLF4J/Logback (backend) and console logging (frontend).
- Include performance monitoring and metrics collection where relevant.
- Validate API requests and responses with comprehensive validation logic.
- Implement proper authentication and authorization using Spring Security.


## General Instructions

- Always prioritize readability and clarity over cleverness.
- For algorithm-related code, include explanations of the approach used.
- Write code with good maintainability practices, including comments on why certain design decisions were made.
- Handle edge cases and write clear exception handling.
- For libraries or external dependencies, mention their usage and purpose only when not obvious.
- Use consistent naming conventions and follow language-specific best practices.
- Write concise, efficient, and idiomatic code that is also easily understandable.
- Avoid obvious or redundant comments that simply restate what the code does.
- Follow SOLID principles and design patterns where appropriate.
- Implement proper separation of concerns between frontend and backend.

## Java Code Style and Formatting

- Follow **Google Java Style Guide** or **Spring conventions**.
- Maintain proper indentation (use 4 spaces for each level of indentation).
- Ensure lines do not exceed 120 characters.
- **Do not add comments that are obvious or redundant** - let the code speak for itself.
- Use blank lines to separate methods, classes, and code blocks where appropriate.
- Use meaningful variable names that indicate purpose (e.g., `userRepository`, `orderService`).
- Follow Java naming conventions:
  - Classes: PascalCase (e.g., `UserController`, `OrderService`)
  - Methods/Variables: camelCase (e.g., `getUserById`, `orderTotal`)
  - Constants: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_ATTEMPTS`)
  - Packages: lowercase (e.g., `com.example.order.service`)

## React/TypeScript Code Style and Formatting

- Follow **Prettier** and **ESLint** configurations.
- Maintain proper indentation (use 2 spaces for each level of indentation).
- Ensure lines do not exceed 100 characters.
- Use meaningful component and variable names.
- Follow React/TypeScript naming conventions:
  - Components: PascalCase (e.g., `UserProfile`, `OrderList`)
  - Functions/Variables: camelCase (e.g., `fetchUserData`, `isLoading`)
  - Constants: UPPER_SNAKE_CASE (e.g., `API_BASE_URL`)
  - Interfaces/Types: PascalCase with 'I' prefix optional (e.g., `User`, `IUserProps`)
- Use arrow functions for functional components.
- Organize imports: React imports, third-party libraries, local imports.

## Java Type Safety and Best Practices

- Use generics for type safety: `List<User>`, `Map<String, Order>`, `Optional<Product>`.
- Use `Optional<T>` instead of returning null for potentially absent values.
- Use `@NotNull`, `@Nullable` annotations from `javax.validation` or Lombok.
- Implement proper validation using Bean Validation (`@Valid`, `@NotBlank`, `@Size`, etc.).
- Use `@RequestBody`, `@PathVariable`, `@RequestParam` appropriately in controllers.
- Implement DTOs (Data Transfer Objects) for API requests/responses.
- Use Lombok annotations to reduce boilerplate (`@Data`, `@Builder`, `@NoArgsConstructor`).

## TypeScript Type Safety and Best Practices

- **All component props must have interface or type definitions**.
- Use strict TypeScript configuration (`strict: true` in `tsconfig.json`).
- Define interfaces for API responses and request payloads.
- Use union types and enums for constrained values.
- Avoid using `any` - prefer `unknown` or specific types.
- Use type guards for runtime type checking.
- Implement proper error boundaries for React components.


## Pre-commit Validation Requirements

The following checks must pass before any commit:

### Java Backend Validation

#### 1. **Maven/Gradle Build** (`mvn clean install` or `./gradlew build`)
- All compilation errors must be resolved
- All unit tests must pass
- Code coverage thresholds met (if configured)

#### 2. **Checkstyle** (Java code style)
- Follow Google Java Style Guide or Spring conventions
- Ensure consistent formatting and naming
- Remove unused imports and variables

#### 3. **SpotBugs/PMD** (Static analysis)
- Fix all bug patterns and code smells
- Address potential null pointer issues
- Resolve security vulnerabilities

#### 4. **JaCoCo** (Code coverage)
- Maintain minimum code coverage (typically 80%)
- Include unit tests for new functionality
- Test edge cases and error scenarios

### React Frontend Validation

#### 1. **ESLint** (JavaScript/TypeScript linting)
- Fix all linting errors and warnings
- Follow Airbnb or Standard style guide
- Remove unused variables and imports
- Ensure proper hook dependencies

#### 2. **Prettier** (Code formatting)
- Automatically formats code to project standards
- Ensures consistent line length and indentation
- Handles proper spacing and bracket placement

#### 3. **TypeScript Compiler** (`tsc`)
- **All components and functions must have proper types**
- Resolve all type incompatibility errors
- Ensure no implicit `any` types
- Validate interface implementations

#### 4. **Unit Tests** (Jest/Vitest + React Testing Library)
- All tests must pass
- Maintain test coverage for components
- Include integration tests for critical flows
- Test user interactions and edge cases

### Shared Validation

#### 5. **Security Scanning** (`OWASP Dependency-Check`, `npm audit`)
- No vulnerable dependencies
- Use environment variables for sensitive data
- Implement proper secrets management
- Regular dependency updates

#### 6. **Integration Tests**
- API contract tests between frontend and backend
- End-to-end tests for critical user flows
- Database migration tests
- Authentication/authorization flow tests


## Configuration and Environment

### Java Spring Boot Configuration
- Use `application.yml` or `application.properties` for configuration.
- Implement profile-specific configurations (dev, test, prod).
- Use `@ConfigurationProperties` for type-safe configuration binding.
- Externalize sensitive data using environment variables or secret managers.
- Validate configuration on startup using `@Validated`.

### React Configuration
- Use `.env` files for environment-specific configuration.
- Prefix public variables with `REACT_APP_` or `VITE_` (depending on build tool).
- Never commit `.env` files with sensitive data.
- Use different `.env` files for different environments (.env.development, .env.production).
- Implement runtime configuration for dynamic settings.

### Shared Best Practices
- Use environment variables for all sensitive data (API keys, database passwords).
- Implement configuration validation and type checking.
- Support multiple deployment environments seamlessly.
- Use proper secrets management (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault).
- Document all required configuration variables.


## Error Handling

### Java Spring Boot Error Handling
- Use `@ControllerAdvice` for global exception handling.
- Create custom exception classes extending `RuntimeException` or specific exceptions.
- Return proper HTTP status codes (400, 404, 500, etc.).
- Use `@ExceptionHandler` for controller-specific error handling.
- Implement consistent error response format (DTOs for error responses).
- Log errors with appropriate severity levels using SLF4J.

### React Error Handling
- Implement Error Boundaries for component-level error catching.
- Use try-catch blocks for async operations.
- Display user-friendly error messages.
- Log errors to monitoring services (Sentry, LogRocket).
- Handle API errors gracefully with proper fallback UI.
- Validate form inputs and provide immediate feedback.

### General Best Practices
- Provide informative, actionable error messages.
- Include stack traces in development, sanitize in production.
- Validate input parameters at entry points.
- Handle edge cases and null/undefined values.
- Implement retry logic for transient failures.


## Refactoring Guidelines

### Java Refactoring
- Break down large methods (>30 lines) into smaller, focused methods.
- Each method should have a single, clear responsibility (SRP).
- Use private methods for internal helper logic.
- Extract common functionality into utility classes or services.
- Prefer composition over inheritance for complex functionality.
- Apply design patterns appropriately (Factory, Strategy, Builder, etc.).

### React Refactoring
- Break down large components (>200 lines) into smaller components.
- Extract custom hooks for reusable stateful logic.
- Use composition patterns (Higher-Order Components, Render Props).
- Separate business logic from presentation (Container/Presentational pattern).
- Create shared utility functions and constants.
- Memoize expensive computations and callbacks.

### General Refactoring Principles
- Eliminate code duplication (DRY principle).
- Improve naming for clarity and intent.
- Remove dead code and unused dependencies.
- Simplify complex conditionals with early returns or guard clauses.
- Refactor incrementally with comprehensive test coverage.


## File Structure and Organization

### Java Spring Boot Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/example/app/
│   │       ├── controller/     # REST controllers
│   │       ├── service/        # Business logic
│   │       ├── repository/     # Data access layer
│   │       ├── model/          # Domain entities
│   │       ├── dto/            # Data transfer objects
│   │       ├── config/         # Configuration classes
│   │       ├── exception/      # Custom exceptions
│   │       └── util/           # Utility classes
│   └── resources/
│       ├── application.yml     # Configuration
│       └── db/migration/       # Database migrations
└── test/
    └── java/                   # Unit and integration tests
```

### React Project Structure
```
src/
├── components/          # Reusable UI components
├── pages/              # Page-level components
├── hooks/              # Custom React hooks
├── services/           # API services
├── utils/              # Utility functions
├── types/              # TypeScript type definitions
├── context/            # React Context providers
├── styles/             # Global styles and themes
└── App.tsx             # Main application component
```

### Best Practices
- Use clear, descriptive file and directory names.
- Group related functionality in dedicated modules/packages.
- Keep configuration files at the project root.
- Separate test files alongside implementation or in dedicated test directories.
- Maintain consistent file naming conventions across the project.


## Dependency Management

### Java (Maven)
```xml
<!-- Use specific versions for production dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.0</version>
</dependency>
```

### Java (Gradle)
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web:3.2.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### React (npm/yarn/pnpm)
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  },
  "devDependencies": {
    "typescript": "^5.0.0",
    "vite": "^5.0.0"
  }
}
```

### Best Practices
- Specify exact versions for production dependencies.
- Use dependency management tools (Maven BOM, Gradle dependency constraints).
- Regularly update dependencies while maintaining compatibility.
- Use lock files (`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`).
- Audit dependencies for security vulnerabilities regularly.
- Keep backend and frontend dependencies synchronized for API contracts.

## Validation and Business Logic

- Implement comprehensive validation for agent responses
- Use structured comparison methods instead of simple keyword matching
- Validate mandatory fields and business rule dependencies
- Provide clear, actionable error messages to users
- Log validation results with appropriate detail levels