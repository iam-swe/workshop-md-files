---
description: 'Java Spring Boot development environment standards - Configuration, profiles, logging, and monitoring'
applyTo: '**/application*.yml, **/application*.properties, **/config/**/*.java, **/logging/**/*.java'
---

# Java Development Environment Standards

## Core Configuration Principles (Non-Negotiable)

- **NEVER hardcode credentials or secrets** - Use environment variables or secret managers
- **NEVER commit sensitive data** - Use .gitignore for local config files
- **ALWAYS use profiles** for different environments (dev, test, prod)
- **NEVER use the same database for different environments**
- **ALWAYS externalize configuration** - Use Spring Boot's configuration hierarchy
- **NEVER log sensitive information** - Mask passwords, tokens, and PII
- **ALWAYS implement structured logging** - Use consistent log formats
- **NEVER use System.out.println()** - Use proper logging framework

## Application Configuration Structure

```yaml
# Good - application.yml (Base configuration)
spring:
  application:
    name: user-service
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  jackson:
    serialization:
      write-dates-as-timestamps: false
      indent-output: true
    time-zone: UTC
    default-property-inclusion: NON_NULL
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: ${SERVER_PORT:8080}
  shutdown: graceful
  servlet:
    context-path: /api
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,application/json
  error:
    include-message: always
    include-binding-errors: always
    include-stacktrace: never  # Never expose stack traces to clients

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: INFO
    com.example: ${LOG_LEVEL:INFO}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

```yaml
# Good - application-dev.yml (Development profile)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb_dev
    username: dev_user
    password: dev_password
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
    show-sql: true
  
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true

logging:
  level:
    com.example: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

server:
  error:
    include-stacktrace: always  # OK for dev
```

```yaml
# Good - application-prod.yml (Production profile)
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate  # Never update schema in production
    properties:
      hibernate:
        format_sql: false
    show-sql: false
  
  cache:
    type: redis
    redis:
      time-to-live: 600000

logging:
  level:
    root: WARN
    com.example: INFO
  file:
    name: /var/log/user-service/application.log
    max-size: 10MB
    max-history: 30

server:
  ssl:
    enabled: true
    key-store: ${SSL_KEY_STORE}
    key-store-password: ${SSL_KEY_STORE_PASSWORD}
    key-store-type: PKCS12
```

```yaml
# Good - application-test.yml (Test profile)
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  
  h2:
    console:
      enabled: true

logging:
  level:
    com.example: DEBUG
```

## Type-Safe Configuration Properties

```java
// Good - Type-safe configuration with validation
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
@Data
public class ApplicationProperties {
    
    @NotNull
    @Valid
    private Security security = new Security();
    
    @NotNull
    @Valid
    private Jwt jwt = new Jwt();
    
    @NotNull
    @Valid
    private Email email = new Email();
    
    @NotNull
    @Valid
    private Storage storage = new Storage();
    
    @Data
    public static class Security {
        @NotBlank
        private String corsAllowedOrigins = "http://localhost:3000";
        
        @Min(3)
        @Max(10)
        private int maxLoginAttempts = 5;
        
        @Min(5)
        @Max(60)
        private int accountLockMinutes = 15;
    }
    
    @Data
    public static class Jwt {
        @NotBlank
        private String secret;
        
        @Min(300000)  // 5 minutes minimum
        private long expiration = 3600000; // 1 hour
        
        @Min(3600000)  // 1 hour minimum
        private long refreshExpiration = 604800000; // 7 days
    }
    
    @Data
    public static class Email {
        @NotBlank
        @Email
        private String from = "noreply@example.com";
        
        @NotBlank
        private String smtpHost = "smtp.gmail.com";
        
        @Min(1)
        @Max(65535)
        private int smtpPort = 587;
        
        @NotBlank
        private String smtpUsername;
        
        @NotBlank
        private String smtpPassword;
        
        private boolean enableTls = true;
    }
    
    @Data
    public static class Storage {
        @NotBlank
        private String uploadDir = "./uploads";
        
        @Min(1)
        private long maxFileSize = 10485760; // 10MB
        
        @NotEmpty
        private List<String> allowedExtensions = Arrays.asList("jpg", "png", "pdf");
    }
}

// Configuration YAML for above
```

```yaml
# application.yml
app:
  security:
    cors-allowed-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
    max-login-attempts: 5
    account-lock-minutes: 15
  
  jwt:
    secret: ${JWT_SECRET}
    expiration: 3600000
    refresh-expiration: 604800000
  
  email:
    from: ${EMAIL_FROM:noreply@example.com}
    smtp-host: ${SMTP_HOST:smtp.gmail.com}
    smtp-port: ${SMTP_PORT:587}
    smtp-username: ${SMTP_USERNAME}
    smtp-password: ${SMTP_PASSWORD}
    enable-tls: true
  
  storage:
    upload-dir: ${UPLOAD_DIR:./uploads}
    max-file-size: 10485760
    allowed-extensions:
      - jpg
      - png
      - pdf
      - docx
```

## Environment Variables

```bash
# Good - .env.example (Template for developers)
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/userdb
DB_USERNAME=your_username
DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your_base64_encoded_secret_key_min_512_bits

# Email Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_specific_password

# AWS Configuration (if using)
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-bucket-name

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=DEBUG
SERVER_PORT=8080
CORS_ORIGINS=http://localhost:3000,http://localhost:5173

# NEVER commit .env with actual values
```

## Logging Standards

```java
// Good - Structured logging with SLF4J
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        try {
            User user = mapToEntity(request);
            User saved = userRepository.save(user);
            
            log.info("User created successfully - ID: {}, Email: {}", 
                saved.getId(), saved.getEmail());
            
            return toResponse(saved);
            
        } catch (DataIntegrityViolationException e) {
            log.error("Failed to create user due to data integrity violation - Email: {}", 
                request.getEmail(), e);
            throw new UserAlreadyExistsException("Email already exists");
            
        } catch (Exception e) {
            log.error("Unexpected error creating user - Email: {}", 
                request.getEmail(), e);
            throw new RuntimeException("Failed to create user", e);
        }
    }
    
    public void processUsers(List<User> users) {
        log.info("Processing {} users", users.size());
        
        int successful = 0;
        int failed = 0;
        
        for (User user : users) {
            try {
                processUser(user);
                successful++;
            } catch (Exception e) {
                failed++;
                log.warn("Failed to process user - ID: {}, Error: {}", 
                    user.getId(), e.getMessage());
            }
        }
        
        log.info("User processing complete - Successful: {}, Failed: {}", 
            successful, failed);
    }
}

// Bad - Poor logging practices
@Service
public class UserService {
    
    public User createUser(CreateUserRequest request) {
        System.out.println("Creating user");  // Use logger
        
        User user = new User();
        user.setPassword(request.getPassword());  // Logging sensitive data
        System.out.println("User: " + user);  // String concatenation, exposes data
        
        try {
            return userRepository.save(user);
        } catch (Exception e) {
            e.printStackTrace();  // Use logger
        }
        
        return null;
    }
}
```

## Custom Logging Configuration

```java
// Good - Logback configuration (logback-spring.xml)
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="applicationName" source="spring.application.name"/>
    
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/${applicationName}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/${applicationName}-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy 
                class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- JSON Appender for production -->
    <appender name="JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/${applicationName}-json.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/${applicationName}-json-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application":"${applicationName}"}</customFields>
        </encoder>
    </appender>
    
    <!-- Development Profile -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
        <logger name="com.example" level="DEBUG"/>
        <logger name="org.springframework.web" level="DEBUG"/>
    </springProfile>
    
    <!-- Production Profile -->
    <springProfile name="prod">
        <root level="WARN">
            <appender-ref ref="FILE"/>
            <appender-ref ref="JSON"/>
        </root>
        <logger name="com.example" level="INFO"/>
    </springProfile>
</configuration>
```

## Actuator Endpoints Configuration

```java
// Good - Custom health indicator
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Available")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}

// Custom info contributor
@Component
public class CustomInfoContributor implements InfoContributor {
    
    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", Map.of(
            "name", "User Service",
            "description", "Manages user accounts and authentication",
            "version", "1.0.0"
        ));
        
        builder.withDetail("java", Map.of(
            "version", System.getProperty("java.version"),
            "vendor", System.getProperty("java.vendor")
        ));
    }
}
```

## Exception Handling Configuration

```java
// Good - Global exception handler with proper logging
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            WebRequest request
    ) {
        log.warn("User not found - Path: {}, Message: {}", 
            request.getDescription(false), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .path(request.getDescription(false))
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? 
                    error.getDefaultMessage() : "Invalid value"
            ));
        
        log.warn("Validation failed - Path: {}, Errors: {}", 
            request.getDescription(false), errors);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed")
            .errors(errors)
            .path(request.getDescription(false))
            .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request
    ) {
        // Log full stack trace but don't expose to client
        log.error("Unexpected error occurred - Path: {}", 
            request.getDescription(false), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getDescription(false))
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

## Performance Monitoring

```java
// Good - Performance monitoring with Micrometer
@Configuration
public class MetricsConfig {
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final MeterRegistry meterRegistry;
    private final UserRepository userRepository;
    
    @Timed(value = "user.create", description = "Time taken to create user")
    public UserResponse createUser(CreateUserRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            User user = mapToEntity(request);
            User saved = userRepository.save(user);
            
            meterRegistry.counter("user.created", "status", "success").increment();
            
            return toResponse(saved);
        } catch (Exception e) {
            meterRegistry.counter("user.created", "status", "failed").increment();
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("user.create.time"));
        }
    }
}
```

## Caching Configuration

```java
// Good - Cache configuration
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("users"),
            new ConcurrentMapCache("roles")
        ));
        return cacheManager;
    }
}

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "users")
public class UserService {
    
    @Cacheable(key = "#id")
    public Optional<UserResponse> findById(Long id) {
        return userRepository.findById(id).map(this::toResponse);
    }
    
    @CachePut(key = "#result.id")
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        // Update logic
    }
    
    @CacheEvict(key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @CacheEvict(allEntries = true)
    public void clearCache() {
        // Clears all user cache
    }
}
```
````
