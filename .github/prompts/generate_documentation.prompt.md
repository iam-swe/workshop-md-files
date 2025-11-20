---
name: "generate_documentation"
description: "Generate detailed documentation for a Java Spring Boot backend + React frontend application."
---

# Instructions for Copilot

You are tasked with creating **comprehensive documentation** for this full-stack Java Spring Boot + React application.erate_documentation"
description: "Generate detailed documentation for a Java Spring Boot backend + React frontend application."
---

# Instructions for Copilot

You are tasked with creating **comprehensive documentation** for this full-stack Java Spring Boot + React application.: "generate_documentation"
description: "Generate detailed documentation about this repository’s structure, code, agents, tools, and tests."
---

# Instructions for Copilot

You are tasked with creating **comprehensive documentation** for this repository.  


## Goals

### 1. **Repository Overview**
   - Explain the purpose and business domain of the application
   - Describe the overall architecture (backend REST API + frontend SPA)
   - Document the tech stack:
     - **Backend**: Java version, Spring Boot version, Spring Security, Spring Data JPA
     - **Frontend**: React version, TypeScript, state management, routing
     - **Database**: PostgreSQL/MySQL, Hibernate/JPA
     - **Build Tools**: Maven/Gradle (backend), npm/yarn/pnpm (frontend)
   - Highlight design patterns (MVC, Repository, DTO, Dependency Injection)
   - Note security, CORS, and profile configurations

### 2. **Backend Structure (Java Spring Boot)**
   - Detailed breakdown of backend directories:
     - `controller/` - REST API endpoints and routing
     - `service/` - Business logic layer
     - `repository/` - JPA repositories and data access
     - `model/entity/` - JPA entities and relationships
     - `dto/` - Request/Response DTOs
     - `config/` - Security, Database, and application configuration
     - `exception/` - Custom exceptions and global error handling
     - `security/` - JWT authentication and authorization
     - `mapper/` - Entity-DTO mappings
     - `util/` - Utility classes
   - Explain layered architecture: Controller → Service → Repository → Database
   - Document dependency injection and component interaction

### 3. **Frontend Structure (React + TypeScript)**
   - Detailed breakdown of frontend directories:
     - `api/` - API client and endpoint definitions
     - `components/` - Reusable React components
     - `pages/` - Page-level views
     - `hooks/` - Custom React hooks
     - `types/` - TypeScript interfaces matching backend DTOs
     - `services/` - Business logic and API integration
     - `context/` - React Context (Auth, Theme)
     - `utils/` - Helper functions
     - `styles/` - Global styles
   - Explain state management approach
   - Document API client setup (Axios/Fetch, interceptors, error handling)

### 4. **API Documentation**
   - List all REST endpoints with:
     - HTTP method, URL path, description
     - Request params, query params, body structure
     - Response format and status codes
     - Authentication/authorization requirements
     - Example requests and responses (JSON)
   - Document API versioning (`/api/v1/`)
   - Explain error response format
   - Note pagination, filtering, sorting patterns

### 5. **Database Schema**
   - Document all tables/entities:
     - Table names, columns (types, constraints)
     - Primary keys, foreign keys, indexes
     - Relationships (One-to-Many, Many-to-Many)
   - Provide ER diagrams or descriptions
   - Document migration scripts (Flyway/Liquibase)
   - Explain naming conventions

### 6. **Security Implementation**
   - Authentication mechanism (JWT, OAuth2, Session)
   - Authorization patterns (roles, permissions)
   - JWT token generation, validation, refresh
   - CORS configuration
   - Password encryption (BCrypt) and validation
   - Security filter chain
   - Rate limiting and brute-force protection
   - Sensitive data handling

### 7. **Configuration & Environment**
   - Document configuration properties:
     - Database connection
     - JWT secrets and expiration
     - Email/SMTP settings
     - File upload limits
     - External API keys
   - Profile-based configs (dev, test, prod)
   - Environment variables template
   - Application setup requirements

### 8. **Testing Strategy**
   - **Backend**: Unit tests (JUnit, Mockito), Integration tests (MockMvc), JaCoCo coverage
   - **Frontend**: Component tests (React Testing Library), E2E tests (Cypress/Playwright)
   - Test naming conventions (methodName_condition_expectedBehavior)
   - AAA pattern (Arrange, Act, Assert)
   - How to run tests and generate coverage reports

### 9. **Build & Deployment**
   - Backend: `mvn clean install`, `mvn package`, Docker
   - Frontend: `npm run build`, environment variables
   - Deployment: JAR, Docker, Kubernetes, CI/CD
   - Database migrations in production
   - Health checks and monitoring

### 10. **Key Classes & Components**
   - Backend: Controllers, Services, Repositories, Entities, Security config
   - Frontend: App routing, Auth components, Pages, API client, Custom hooks
   - Document purpose, responsibilities, and interactions

### 11. **Workflows & Data Flow**
   - User registration, login, CRUD operations
   - Request flow: Frontend → API → Controller → Service → Repository → DB
   - State management flow
   - Error handling at each layer
   - Sequence diagrams for complex flows

### 12. **Best Practices & Standards**
   - Reference `.github/instructions/` files
   - Java/TypeScript naming conventions
   - Transaction management
   - Exception handling
   - Logging standards (SLF4J)
   - Type safety and validation

## Output Format

- `#` for major sections, `##` for subsections, `###` for details
- Code examples with syntax highlighting (```java, ```typescript, ```yaml)
- Tables for endpoints, configs, environment variables
- Mermaid diagrams for architecture and flows
- Actual file paths and line references
- Example JSON requests/responses
- Important notes callouts

## Writing Style

- Professional, clear, detailed (not superficial)
- Explain **WHY** decisions were made, not just **WHAT**
- Assume reader is a new developer on the project
- Include real codebase examples
- Highlight security and performance considerations
- Cross-reference related sections
