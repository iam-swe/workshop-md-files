````instructions
---
description: 'Java Spring Boot testing standards - Unit, Integration, and E2E testing'
applyTo: '**/*Test.java, **/*IT.java, **/test/**/*.java'
---

# Java Testing Standards

## Core Testing Principles (Non-Negotiable)

- **NEVER skip tests** - All new code must have corresponding tests
- **NEVER use real databases in unit tests** - Use mocks or in-memory databases
- **NEVER share state between tests** - Each test must be independent
- **ALWAYS follow AAA pattern** - Arrange, Act, Assert
- **ALWAYS use descriptive test names** - Method name should describe what's being tested
- **NEVER ignore failing tests** - Fix or remove, never `@Disabled` without a ticket
- **ALWAYS test edge cases** - Null values, empty collections, boundary conditions

## Test Naming Conventions

```java
// ✅ Good - Descriptive test names following pattern: methodName_condition_expectedBehavior
@Test
void createUser_WithValidRequest_ShouldCreateUserAndReturnResponse() { }

@Test
void createUser_WithDuplicateEmail_ShouldThrowUserAlreadyExistsException() { }

@Test
void findUserById_WhenUserDoesNotExist_ShouldReturnEmptyOptional() { }

@Test
void updateUser_WithInvalidId_ShouldThrowUserNotFoundException() { }

// ❌ Bad - Vague test names
@Test
void testCreateUser() { }

@Test
void test1() { }

@Test
void userTest() { }
```

## Unit Testing Standards

### Service Layer Tests

```java
// ✅ Good - Comprehensive service test with mocks
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserService userService;
    
    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {
        
        private CreateUserRequest validRequest;
        private User user;
        private UserResponse expectedResponse;
        
        @BeforeEach
        void setUp() {
            validRequest = CreateUserRequest.builder()
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .build();
            
            user = User.builder()
                .id(1L)
                .email(validRequest.getEmail())
                .name(validRequest.getName())
                .role(validRequest.getRole())
                .build();
            
            expectedResponse = UserResponse.builder()
                .id(1L)
                .email(validRequest.getEmail())
                .name(validRequest.getName())
                .role(validRequest.getRole())
                .build();
        }
        
        @Test
        void createUser_WithValidRequest_ShouldCreateUserAndReturnResponse() {
            // Arrange
            when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
            when(userMapper.toEntity(validRequest)).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(expectedResponse);
            
            // Act
            UserResponse actual = userService.createUser(validRequest);
            
            // Assert
            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo(expectedResponse.getId());
            assertThat(actual.getEmail()).isEqualTo(expectedResponse.getEmail());
            assertThat(actual.getName()).isEqualTo(expectedResponse.getName());
            
            verify(userRepository).existsByEmail(validRequest.getEmail());
            verify(userRepository).save(user);
            verify(emailService).sendWelcomeEmail(user.getEmail());
            verifyNoMoreInteractions(userRepository, emailService);
        }
        
        @Test
        void createUser_WithDuplicateEmail_ShouldThrowException() {
            // Arrange
            when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);
            
            // Act & Assert
            assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User with email already exists");
            
            verify(userRepository).existsByEmail(validRequest.getEmail());
            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendWelcomeEmail(anyString());
        }
        
        @Test
        void createUser_WithNullEmail_ShouldThrowException() {
            // Arrange
            validRequest.setEmail(null);
            
            // Act & Assert
            assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(IllegalArgumentException.class);
            
            verify(userRepository, never()).save(any(User.class));
        }
    }
    
    @Nested
    @DisplayName("Find User Tests")
    class FindUserTests {
        
        @Test
        void findById_WhenUserExists_ShouldReturnUser() {
            // Arrange
            Long userId = 1L;
            User user = User.builder().id(userId).build();
            UserResponse expectedResponse = UserResponse.builder().id(userId).build();
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(expectedResponse);
            
            // Act
            Optional<UserResponse> actual = userService.findById(userId);
            
            // Assert
            assertThat(actual).isPresent();
            assertThat(actual.get().getId()).isEqualTo(userId);
            verify(userRepository).findById(userId);
        }
        
        @Test
        void findById_WhenUserDoesNotExist_ShouldReturnEmptyOptional() {
            // Arrange
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            
            // Act
            Optional<UserResponse> actual = userService.findById(userId);
            
            // Assert
            assertThat(actual).isEmpty();
            verify(userRepository).findById(userId);
            verify(userMapper, never()).toResponse(any());
        }
    }
    
    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {
        
        @Test
        void updateUser_WithValidData_ShouldUpdateAndReturnUser() {
            // Arrange
            Long userId = 1L;
            UpdateUserRequest request = UpdateUserRequest.builder()
                .name("Updated Name")
                .build();
            
            User existingUser = User.builder()
                .id(userId)
                .name("Old Name")
                .email("test@example.com")
                .build();
            
            User updatedUser = User.builder()
                .id(userId)
                .name(request.getName())
                .email("test@example.com")
                .build();
            
            UserResponse expectedResponse = UserResponse.builder()
                .id(userId)
                .name(request.getName())
                .build();
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(updatedUser);
            when(userMapper.toResponse(updatedUser)).thenReturn(expectedResponse);
            
            // Act
            UserResponse actual = userService.updateUser(userId, request);
            
            // Assert
            assertThat(actual.getName()).isEqualTo(request.getName());
            verify(userRepository).findById(userId);
            verify(userMapper).updateEntity(existingUser, request);
            verify(userRepository).save(existingUser);
        }
        
        @Test
        void updateUser_WithInvalidId_ShouldThrowException() {
            // Arrange
            Long invalidId = 999L;
            UpdateUserRequest request = UpdateUserRequest.builder().name("New Name").build();
            when(userRepository.findById(invalidId)).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThatThrownBy(() -> userService.updateUser(invalidId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(invalidId.toString());
            
            verify(userRepository).findById(invalidId);
            verify(userRepository, never()).save(any());
        }
    }
}

// ❌ Bad - Poor testing practices
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void test() {  // ❌ Non-descriptive name
        User user = new User();
        when(userRepository.save(user)).thenReturn(user);
        userService.createUser(null);  // ❌ No assertions
    }
    
    @Test
    void testGetUser() {
        // ❌ No arrange section
        userService.findById(1L);  // ❌ No assertions, no verification
    }
}
```

## Integration Testing Standards

```java
// ✅ Good - Comprehensive integration test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Nested
    @DisplayName("POST /api/v1/users - Create User")
    class CreateUserTests {
        
        @Test
        void createUser_WithValidRequest_ShouldReturn201AndCreatedUser() throws Exception {
            // Arrange
            CreateUserRequest request = CreateUserRequest.builder()
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .phoneNumber("+1234567890")
                .build();
            
            // Act & Assert
            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.role").value(request.getRole().toString()))
                .andExpect(jsonPath("$.phoneNumber").value(request.getPhoneNumber()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
            
            // Verify database state
            assertThat(userRepository.findByEmail(request.getEmail())).isPresent();
        }
        
        @Test
        void createUser_WithDuplicateEmail_ShouldReturn400() throws Exception {
            // Arrange
            User existingUser = User.builder()
                .email("existing@example.com")
                .name("Existing User")
                .role(UserRole.USER)
                .build();
            userRepository.save(existingUser);
            
            CreateUserRequest request = CreateUserRequest.builder()
                .email(existingUser.getEmail())
                .name("New User")
                .role(UserRole.USER)
                .build();
            
            // Act & Assert
            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User with email already exists"));
        }
        
        @Test
        void createUser_WithInvalidEmail_ShouldReturn400WithValidationErrors() throws Exception {
            // Arrange
            CreateUserRequest request = CreateUserRequest.builder()
                .email("invalid-email")  // Invalid format
                .name("Test User")
                .role(UserRole.USER)
                .build();
            
            // Act & Assert
            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists());
        }
        
        @Test
        void createUser_WithMissingRequiredFields_ShouldReturn400() throws Exception {
            // Arrange
            CreateUserRequest request = CreateUserRequest.builder()
                .email("")  // Empty email
                .name("")   // Empty name
                .build();
            
            // Act & Assert
            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.role").exists());
        }
    }
    
    @Nested
    @DisplayName("GET /api/v1/users/{id} - Get User")
    class GetUserTests {
        
        @Test
        void getUser_WhenUserExists_ShouldReturn200AndUser() throws Exception {
            // Arrange
            User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .build();
            User savedUser = userRepository.save(user);
            
            // Act & Assert
            mockMvc.perform(get("/api/v1/users/{id}", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.email").value(savedUser.getEmail()))
                .andExpect(jsonPath("$.name").value(savedUser.getName()));
        }
        
        @Test
        void getUser_WhenUserDoesNotExist_ShouldReturn404() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/users/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
        }
        
        @Test
        void getUser_WithInvalidId_ShouldReturn400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/users/{id}", -1L))
                .andExpect(status().isBadRequest());
        }
    }
    
    @Nested
    @DisplayName("PUT /api/v1/users/{id} - Update User")
    class UpdateUserTests {
        
        @Test
        void updateUser_WithValidData_ShouldReturn200AndUpdatedUser() throws Exception {
            // Arrange
            User user = userRepository.save(User.builder()
                .email("test@example.com")
                .name("Old Name")
                .role(UserRole.USER)
                .build());
            
            UpdateUserRequest request = UpdateUserRequest.builder()
                .name("New Name")
                .role(UserRole.ADMIN)
                .build();
            
            // Act & Assert
            mockMvc.perform(put("/api/v1/users/{id}", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.role").value(request.getRole().toString()))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
            
            // Verify database state
            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.getName()).isEqualTo(request.getName());
            assertThat(updatedUser.getRole()).isEqualTo(request.getRole());
        }
    }
    
    @Nested
    @DisplayName("DELETE /api/v1/users/{id} - Delete User")
    class DeleteUserTests {
        
        @Test
        void deleteUser_WhenUserExists_ShouldReturn204() throws Exception {
            // Arrange
            User user = userRepository.save(User.builder()
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .build());
            
            // Act & Assert
            mockMvc.perform(delete("/api/v1/users/{id}", user.getId()))
                .andExpect(status().isNoContent());
            
            // Verify database state
            assertThat(userRepository.findById(user.getId())).isEmpty();
        }
        
        @Test
        void deleteUser_WhenUserDoesNotExist_ShouldReturn404() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/users/{id}", 999L))
                .andExpect(status().isNotFound());
        }
    }
}
```

## Repository Testing Standards

```java
// ✅ Good - Repository test with test containers or in-memory DB
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Nested
    @DisplayName("Find By Email Tests")
    class FindByEmailTests {
        
        @Test
        void findByEmail_WhenUserExists_ShouldReturnUser() {
            // Arrange
            User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .build();
            entityManager.persistAndFlush(user);
            
            // Act
            Optional<User> found = userRepository.findByEmail("test@example.com");
            
            // Assert
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo(user.getEmail());
            assertThat(found.get().getName()).isEqualTo(user.getName());
        }
        
        @Test
        void findByEmail_WhenUserDoesNotExist_ShouldReturnEmpty() {
            // Act
            Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
            
            // Assert
            assertThat(found).isEmpty();
        }
        
        @Test
        void findByEmail_WithCaseInsensitiveEmail_ShouldReturnUser() {
            // Arrange
            User user = User.builder()
                .email("Test@Example.com")
                .name("Test User")
                .role(UserRole.USER)
                .build();
            entityManager.persistAndFlush(user);
            
            // Act
            Optional<User> found = userRepository.findByEmail("test@example.com");
            
            // Assert
            assertThat(found).isPresent();
        }
    }
    
    @Test
    void existsByEmail_WhenUserExists_ShouldReturnTrue() {
        // Arrange
        User user = User.builder()
            .email("test@example.com")
            .name("Test User")
            .role(UserRole.USER)
            .build();
        entityManager.persistAndFlush(user);
        
        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");
        
        // Assert
        assertThat(exists).isTrue();
    }
    
    @Test
    void existsByEmail_WhenUserDoesNotExist_ShouldReturnFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");
        
        // Assert
        assertThat(exists).isFalse();
    }
    
    @Test
    void findActiveUsersSince_ShouldReturnOnlyActiveUsersAfterDate() {
        // Arrange
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        
        User activeRecent = User.builder()
            .email("active@example.com")
            .name("Active User")
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now().minusDays(3))
            .build();
        
        User activeOld = User.builder()
            .email("old@example.com")
            .name("Old User")
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now().minusDays(30))
            .build();
        
        User inactiveRecent = User.builder()
            .email("inactive@example.com")
            .name("Inactive User")
            .role(UserRole.USER)
            .status(UserStatus.INACTIVE)
            .createdAt(LocalDateTime.now().minusDays(3))
            .build();
        
        entityManager.persist(activeRecent);
        entityManager.persist(activeOld);
        entityManager.persist(inactiveRecent);
        entityManager.flush();
        
        // Act
        List<User> users = userRepository.findActiveUsersSince(UserStatus.ACTIVE, cutoffDate);
        
        // Assert
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo(activeRecent.getEmail());
    }
}
```

## Test Configuration

```java
// ✅ Good - Test configuration class
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(
            Instant.parse("2024-01-01T00:00:00Z"),
            ZoneId.of("UTC")
        );
    }
    
    @Bean
    @Primary
    public EmailService mockEmailService() {
        return Mockito.mock(EmailService.class);
    }
}
```

## Test Data Builders

```java
// ✅ Good - Test data builder for consistent test data
public class UserTestDataBuilder {
    
    private Long id = 1L;
    private String email = "test@example.com";
    private String name = "Test User";
    private UserRole role = UserRole.USER;
    private String phoneNumber = "+1234567890";
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }
    
    public UserTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }
    
    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public UserTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public UserTestDataBuilder withRole(UserRole role) {
        this.role = role;
        return this;
    }
    
    public UserTestDataBuilder asAdmin() {
        this.role = UserRole.ADMIN;
        return this;
    }
    
    public User build() {
        return User.builder()
            .id(id)
            .email(email)
            .name(name)
            .role(role)
            .phoneNumber(phoneNumber)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }
    
    public CreateUserRequest buildRequest() {
        return CreateUserRequest.builder()
            .email(email)
            .name(name)
            .role(role)
            .phoneNumber(phoneNumber)
            .build();
    }
}

// Usage in tests
@Test
void testWithBuilder() {
    User user = aUser()
        .withEmail("custom@example.com")
        .asAdmin()
        .build();
    
    CreateUserRequest request = aUser()
        .withName("John Doe")
        .buildRequest();
}
```

## Parameterized Tests

```java
// ✅ Good - Parameterized tests for multiple scenarios
class UserValidationTest {
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    void validateEmail_WithBlankEmail_ShouldThrowException(String email) {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
            .email(email)
            .name("Test User")
            .build();
        
        // Act & Assert
        assertThatThrownBy(() -> userValidator.validate(request))
            .isInstanceOf(ValidationException.class);
    }
    
    @ParameterizedTest
    @CsvSource({
        "invalid-email, false",
        "test@, false",
        "@example.com, false",
        "test@example.com, true",
        "user.name@example.co.uk, true"
    })
    void validateEmail_WithVariousFormats_ShouldValidateCorrectly(String email, boolean expected) {
        // Act
        boolean isValid = emailValidator.isValid(email);
        
        // Assert
        assertThat(isValid).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @EnumSource(UserRole.class)
    void createUser_WithAllRoles_ShouldCreateSuccessfully(UserRole role) {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
            .email("test@example.com")
            .name("Test User")
            .role(role)
            .build();
        
        when(userRepository.save(any())).thenReturn(new User());
        
        // Act & Assert
        assertThatCode(() -> userService.createUser(request))
            .doesNotThrowAnyException();
    }
    
    @ParameterizedTest
    @MethodSource("provideInvalidUserRequests")
    void createUser_WithInvalidRequests_ShouldThrowException(
        CreateUserRequest request, 
        Class<? extends Exception> expectedException
    ) {
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(expectedException);
    }
    
    private static Stream<Arguments> provideInvalidUserRequests() {
        return Stream.of(
            Arguments.of(
                CreateUserRequest.builder().email(null).build(),
                IllegalArgumentException.class
            ),
            Arguments.of(
                CreateUserRequest.builder().email("").build(),
                ValidationException.class
            ),
            Arguments.of(
                CreateUserRequest.builder().email("invalid").build(),
                ValidationException.class
            )
        );
    }
}
```

## Test Coverage Standards

```java
// Minimum coverage requirements:
// - Line Coverage: 80%
// - Branch Coverage: 75%
// - Method Coverage: 90%

// JaCoCo configuration in pom.xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.75</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Test Containers for Integration Tests

```java
// ✅ Good - Using Testcontainers for real database testing
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    void createUser_WithValidRequest_ShouldPersistToDatabase() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
            .email("test@example.com")
            .name("Test User")
            .role(UserRole.USER)
            .build();
        
        // Act
        UserResponse response = userService.createUser(request);
        
        // Assert
        assertThat(response.getId()).isNotNull();
        
        Optional<User> savedUser = userRepository.findById(response.getId());
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEmail()).isEqualTo(request.getEmail());
    }
}
```

## Performance Testing

```java
// ✅ Good - Performance test example
@SpringBootTest
class UserServicePerformanceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void findAllUsers_WithLargeDataset_ShouldCompleteWithinTimeout() {
        // Arrange
        // Create 10000 test users
        
        // Act
        List<UserResponse> users = userService.findAll(PageRequest.of(0, 100));
        
        // Assert
        assertThat(users).isNotEmpty();
    }
    
    @Test
    void bulkCreateUsers_ShouldUseBatchProcessing() {
        // Arrange
        List<CreateUserRequest> requests = IntStream.range(0, 1000)
            .mapToObj(i -> CreateUserRequest.builder()
                .email("user" + i + "@example.com")
                .name("User " + i)
                .role(UserRole.USER)
                .build())
            .toList();
        
        // Act
        long startTime = System.currentTimeMillis();
        userService.bulkCreate(requests);
        long endTime = System.currentTimeMillis();
        
        // Assert
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(5000); // Should complete in less than 5 seconds
    }
}
```

## Common Testing Patterns

```java
// ✅ Verify method called with specific matcher
verify(userRepository).save(argThat(user -> 
    user.getEmail().equals("test@example.com") &&
    user.getRole() == UserRole.USER
));

// ✅ Capture argument for detailed assertion
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
verify(userRepository).save(userCaptor.capture());
User capturedUser = userCaptor.getValue();
assertThat(capturedUser.getEmail()).isEqualTo("test@example.com");

// ✅ Verify multiple invocations
verify(userRepository, times(2)).save(any(User.class));
verify(userRepository, atLeast(1)).findById(anyLong());
verify(userRepository, never()).deleteById(anyLong());

// ✅ Verify invocation order
InOrder inOrder = inOrder(userRepository, emailService);
inOrder.verify(userRepository).save(any(User.class));
inOrder.verify(emailService).sendWelcomeEmail(anyString());

// ✅ Custom matchers
when(userRepository.findById(longThat(id -> id > 0 && id < 1000)))
    .thenReturn(Optional.of(new User()));
```
````