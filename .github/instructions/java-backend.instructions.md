````instructions
---
description: 'Java Spring Boot backend development standards and best practices'
applyTo: '**/*.java, **/pom.xml, **/application.yml, **/application.properties'
---

# Java Spring Boot Backend Standards

## Core Java Standards (Non-Negotiable)

- **NEVER use raw types** - Always use generics: `List<User>`, `Map<String, Object>`
- **NEVER return null** - Use `Optional<T>` for potentially absent values
- **NEVER catch generic Exception** - Catch specific exceptions or create custom ones
- **NEVER use `@Autowired` on fields** - Use constructor injection for better testability
- **NEVER hardcode configuration** - Use `@Value` or `@ConfigurationProperties`
- **ALWAYS validate inputs** - Use Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`)

## Project Structure

```
src/
├── main/
│   ├── java/com/company/project/
│   │   ├── config/          # Configuration classes (@Configuration)
│   │   ├── controller/      # REST controllers (@RestController)
│   │   ├── service/         # Business logic (@Service)
│   │   ├── repository/      # Data access layer (@Repository)
│   │   ├── model/           # JPA entities (@Entity)
│   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── request/     # API request DTOs
│   │   │   └── response/    # API response DTOs
│   │   ├── exception/       # Custom exceptions
│   │   ├── mapper/          # Entity-DTO mappers (MapStruct)
│   │   ├── security/        # Security configuration
│   │   └── util/            # Utility classes
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── db/migration/    # Flyway/Liquibase migrations
└── test/
    └── java/                # Unit and integration tests
```

## Naming Conventions

- **Classes**: PascalCase with clear suffixes
  - Controllers: `UserController`, `OrderController`
  - Services: `UserService`, `OrderService`
  - Repositories: `UserRepository`, `OrderRepository`
  - DTOs: `CreateUserRequest`, `UserResponse`
  - Entities: `User`, `Order`, `Product`
  - Exceptions: `UserNotFoundException`, `InvalidOrderException`
- **Methods**: camelCase, verb-based
  - `getUserById`, `createOrder`, `validatePayment`
- **Constants**: UPPER_SNAKE_CASE
  - `MAX_RETRY_ATTEMPTS`, `DEFAULT_PAGE_SIZE`
- **Packages**: lowercase, singular
  - `com.company.user.service`, `com.company.order.controller`

## Controller Layer Standards

```java
// ✅ Good - Clean, RESTful controller
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable @Positive Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable @Positive Long id,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable @Positive Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

// ❌ Bad - Multiple issues
@RestController
public class UserController {
    
    @Autowired  // ❌ Field injection
    private UserService userService;
    
    @GetMapping("/getUser")  // ❌ Verb in URL, no versioning
    public User getUser(@PathVariable Long id) {  // ❌ Returns entity, no validation
        return userService.findById(id).orElse(null);  // ❌ Returns null
    }
    
    @PostMapping("/user")  // ❌ No validation
    public User createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request);  // ❌ No proper status code
    }
}
```

## Service Layer Standards

```java
// ✅ Good - Clean service with proper error handling
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    public Optional<UserResponse> findById(Long id) {
        return userRepository.findById(id)
            .map(userMapper::toResponse);
    }
    
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email already exists");
        }
        
        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        
        log.info("Created user with id: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }
    
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        
        userMapper.updateEntity(user, request);
        User updatedUser = userRepository.save(user);
        
        log.info("Updated user with id: {}", id);
        return userMapper.toResponse(updatedUser);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user with id: {}", id);
    }
}

// ❌ Bad - Multiple issues
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User getUser(Long id) {  // ❌ Returns entity
        return userRepository.findById(id).orElse(null);  // ❌ Returns null
    }
    
    public User createUser(CreateUserRequest request) {  // ❌ No validation
        User user = new User();  // ❌ Manual mapping
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        return userRepository.save(user);  // ❌ No logging, returns entity
    }
}
```

## Repository Layer Standards

```java
// ✅ Good - Clean repository with custom queries
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.createdAt > :since")
    List<User> findActiveUsersSince(
        @Param("status") UserStatus status,
        @Param("since") LocalDateTime since
    );
    
    @Query(value = "SELECT * FROM users u WHERE u.role = ?1 ORDER BY u.created_at DESC LIMIT ?2",
           nativeQuery = true)
    List<User> findTopUsersByRole(String role, int limit);
}

// ❌ Bad
public interface UserRepository extends CrudRepository<User, Long> {  // ❌ Use JpaRepository
    
    User findByEmail(String email);  // ❌ Should return Optional<User>
    
    @Query("SELECT u FROM User u WHERE u.email = ?1")  // ❌ Unnecessary query
    User getByEmail(String email);
}
```

## DTO Standards

```java
// ✅ Good - Clean DTOs with validation
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @NotNull(message = "Role is required")
    private UserRole role;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phoneNumber;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// ❌ Bad - No validation, mutable
public class CreateUserRequest {
    public String email;  // ❌ Public fields, no validation
    public String name;
}
```

## Entity Standards

```java
// ✅ Good - Clean entity with proper relationships
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// ❌ Bad
@Entity
public class User {
    @Id
    private Long id;  // ❌ No generation strategy
    private String email;  // ❌ No constraints
    private String name;
    // ❌ No timestamps, no indexes
}
```

## Exception Handling

```java
// ✅ Good - Global exception handler
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.error("User not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
            ));
        
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message("Validation failed")
            .errors(errors)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// Custom exception
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super(String.format("User not found with id: %d", id));
    }
    
    public UserNotFoundException(String message) {
        super(message);
    }
}
```

## Configuration Standards

```yaml
# application.yml - ✅ Good
spring:
  application:
    name: user-service
  
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/userdb}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        default_schema: public
    show-sql: false
  
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: UTC

logging:
  level:
    com.company.project: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: DEBUG

server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /api
```

## Dependency Injection

```java
// ✅ Good - Constructor injection
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;
}

// ✅ Good - Multiple implementations with @Qualifier
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    @Qualifier("emailNotifier")
    private final Notifier emailNotifier;
    
    @Qualifier("smsNotifier")
    private final Notifier smsNotifier;
}

// ❌ Bad - Field injection
@RestController
public class UserController {
    @Autowired  // ❌ Don't use field injection
    private UserService userService;
}
```

## Testing Standards

```java
// ✅ Good - Unit test
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserMapper userMapper;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void createUser_WithValidRequest_ShouldCreateUser() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
            .email("test@example.com")
            .name("Test User")
            .role(UserRole.USER)
            .build();
        
        User user = User.builder()
            .id(1L)
            .email(request.getEmail())
            .name(request.getName())
            .role(request.getRole())
            .build();
        
        UserResponse expected = UserResponse.builder()
            .id(1L)
            .email(request.getEmail())
            .name(request.getName())
            .role(request.getRole())
            .build();
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expected);
        
        // When
        UserResponse actual = userService.createUser(request);
        
        // Then
        assertThat(actual).isEqualTo(expected);
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).save(user);
    }
    
    @Test
    void createUser_WithExistingEmail_ShouldThrowException() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
            .email("test@example.com")
            .build();
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessage("User with email already exists");
    }
}

// ✅ Good - Integration test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void createUser_WithValidRequest_ShouldReturn201() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
            .email("test@example.com")
            .name("Test User")
            .role(UserRole.USER)
            .build();
        
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(request.getEmail()))
            .andExpect(jsonPath("$.name").value(request.getName()));
    }
}
```

## Security Standards

```java
// ✅ Good - Security configuration
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

## Performance Best Practices

- **Use pagination** for list endpoints: `Pageable`, `Page<T>`
- **Use projections** for read-only queries
- **Enable caching** with `@Cacheable`, `@CacheEvict`
- **Use batch operations** for bulk inserts/updates
- **Optimize N+1 queries** with `@EntityGraph` or JOIN FETCH
- **Use connection pooling** (HikariCP is default)
- **Index frequently queried columns**

## Logging Standards

```java
// ✅ Good - Structured logging
@Slf4j
@Service
public class UserService {
    
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        try {
            // Business logic
            log.info("User created successfully with id: {}", user.getId());
            return response;
        } catch (Exception e) {
            log.error("Failed to create user with email: {}", request.getEmail(), e);
            throw e;
        }
    }
}

// ❌ Bad
System.out.println("Creating user");  // ❌ Use logger
log.info("User: " + user);  // ❌ Use parameterized logging
log.debug("Full user object: {}", user);  // ❌ Can expose sensitive data
```
````