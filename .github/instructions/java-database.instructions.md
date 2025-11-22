---
description: 'Java Spring Boot database and JPA standards - Repository patterns, query optimization, and data access'
applyTo: '**/repository/**/*.java, **/entity/**/*.java, **/model/**/*.java, **/migration/**/*.sql'
---

# Java Database and JPA Standards

## Core Database Principles (Non-Negotiable)

- **NEVER use native queries without good reason** - Use JPQL or Criteria API first
- **NEVER use `@GeneratedValue` without strategy** - Always specify IDENTITY, SEQUENCE, or TABLE
- **ALWAYS use indexes** on frequently queried columns
- **NEVER fetch collections eagerly** - Use LAZY loading and fetch joins when needed
- **ALWAYS use database transactions appropriately** - Mark read-only where applicable
- **NEVER concatenate SQL strings** - Use parameterized queries always
- **ALWAYS use pagination** for list queries
- **NEVER ignore N+1 query problems** - Use @EntityGraph or JOIN FETCH

## Entity Design Standards

```java
// ✅ Good - Well-designed entity with proper annotations
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_status", columnList = "status"),
        @Index(name = "idx_user_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_email", columnNames = "email")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    @Email(message = "Invalid email format")
    private String email;
    
    @Column(name = "name", nullable = false, length = 100)
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Column(name = "password", nullable = false, length = 255)
    @JsonIgnore
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;
    
    @Column(name = "phone_number", length = 20)
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phoneNumber;
    
    @Column(name = "enabled", nullable = false)
    private boolean enabled;
    
    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // One-to-Many relationship
    @OneToMany(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();
    
    // Many-to-One relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", foreignKey = @ForeignKey(name = "fk_user_department"))
    private Department department;
    
    // Many-to-Many relationship
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_role_user")),
        inverseJoinColumns = @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_role_role"))
    )
    private Set<Role> roles = new HashSet<>();
    
    // Helper methods for bidirectional relationships
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }
    
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
        enabled = true;
        accountNonLocked = true;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// ❌ Bad - Poorly designed entity
@Entity
@Table(name = "users")  // ❌ No indexes
public class User {
    
    @Id
    @GeneratedValue  // ❌ No strategy specified
    private Long id;
    
    private String email;  // ❌ No constraints
    private String name;
    private String password;  // ❌ Not hidden from JSON
    
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)  // ❌ Eager fetching
    private List<Order> orders;
    
    // ❌ No timestamps, no version control
}
```

## Repository Interface Standards

```java
// ✅ Good - Well-designed repository with custom queries
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Simple derived query methods
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<User> findByStatus(UserStatus status);
    
    List<User> findByRoleAndStatus(UserRole role, UserStatus status);
    
    // JPQL queries
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.createdAt > :since")
    List<User> findActiveUsersSince(
        @Param("status") UserStatus status,
        @Param("since") LocalDateTime since
    );
    
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Query with JOIN FETCH to avoid N+1 problem
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :userId")
    Optional<User> findByIdWithOrders(@Param("userId") Long userId);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.status = :status")
    List<User> findByStatusWithDepartment(@Param("status") UserStatus status);
    
    // Projection query for better performance
    @Query("SELECT new com.example.dto.UserSummaryDTO(u.id, u.name, u.email) " +
           "FROM User u WHERE u.status = :status")
    List<UserSummaryDTO> findUserSummaries(@Param("status") UserStatus status);
    
    // Native query (use sparingly)
    @Query(
        value = "SELECT * FROM users u WHERE u.created_at > :date ORDER BY u.created_at DESC LIMIT :limit",
        nativeQuery = true
    )
    List<User> findRecentUsers(
        @Param("date") LocalDateTime date,
        @Param("limit") int limit
    );
    
    // Modifying query
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
    int updateUserStatus(@Param("userId") Long userId, @Param("status") UserStatus status);
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.status = :status AND u.createdAt < :date")
    int deleteInactiveUsersBefore(@Param("status") UserStatus status, @Param("date") LocalDateTime date);
    
    // Using EntityGraph to fetch associations
    @EntityGraph(attributePaths = {"orders", "department"})
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdWithAssociations(@Param("userId") Long userId);
    
    // Count query
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND u.role = :role")
    long countByStatusAndRole(@Param("status") UserStatus status, @Param("role") UserRole role);
}

// ❌ Bad - Poorly designed repository
@Repository
public interface UserRepository extends CrudRepository<User, Long> {  // ❌ Use JpaRepository
    
    User findByEmail(String email);  // ❌ Should return Optional
    
    @Query("SELECT * FROM users WHERE email = ?1")  // ❌ Native query for simple case
    User getByEmail(String email);
    
    @Query("SELECT u FROM User u")  // ❌ Fetches all without pagination
    List<User> getAllUsers();
}
```

## Custom Repository Implementation

```java
// ✅ Good - Custom repository for complex queries
public interface UserRepositoryCustom {
    List<User> findUsersByComplexCriteria(UserSearchCriteria criteria);
    Page<User> searchUsersWithFilters(UserSearchCriteria criteria, Pageable pageable);
}

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {
    
    @PersistenceContext
    private final EntityManager entityManager;
    
    @Override
    public List<User> findUsersByComplexCriteria(UserSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Add search criteria dynamically
        if (criteria.getEmail() != null && !criteria.getEmail().isEmpty()) {
            predicates.add(cb.like(
                cb.lower(user.get("email")),
                "%" + criteria.getEmail().toLowerCase() + "%"
            ));
        }
        
        if (criteria.getName() != null && !criteria.getName().isEmpty()) {
            predicates.add(cb.like(
                cb.lower(user.get("name")),
                "%" + criteria.getName().toLowerCase() + "%"
            ));
        }
        
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(user.get("status"), criteria.getStatus()));
        }
        
        if (criteria.getRole() != null) {
            predicates.add(cb.equal(user.get("role"), criteria.getRole()));
        }
        
        if (criteria.getCreatedAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                user.get("createdAt"),
                criteria.getCreatedAfter()
            ));
        }
        
        if (criteria.getCreatedBefore() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                user.get("createdAt"),
                criteria.getCreatedBefore()
            ));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(user.get("createdAt")));
        
        return entityManager.createQuery(query).getResultList();
    }
    
    @Override
    public Page<User> searchUsersWithFilters(UserSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Query for data
        CriteriaQuery<User> dataQuery = cb.createQuery(User.class);
        Root<User> dataRoot = dataQuery.from(User.class);
        dataQuery.where(buildPredicates(cb, dataRoot, criteria).toArray(new Predicate[0]));
        
        TypedQuery<User> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        // Query for count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<User> countRoot = countQuery.from(User.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, criteria).toArray(new Predicate[0]));
        
        Long total = entityManager.createQuery(countQuery).getSingleResult();
        
        return new PageImpl<>(typedQuery.getResultList(), pageable, total);
    }
    
    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<User> root,
            UserSearchCriteria criteria
    ) {
        List<Predicate> predicates = new ArrayList<>();
        
        if (criteria.getEmail() != null) {
            predicates.add(cb.like(cb.lower(root.get("email")), 
                "%" + criteria.getEmail().toLowerCase() + "%"));
        }
        
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }
        
        return predicates;
    }
}
```

## Transaction Management

```java
// ✅ Good - Proper transaction management
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)  // Default to read-only
public class UserService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    
    // Read-only operation - uses class-level @Transactional(readOnly = true)
    public Optional<UserResponse> findById(Long id) {
        return userRepository.findById(id)
            .map(this::toResponse);
    }
    
    // Write operation - override with @Transactional
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User user = mapToEntity(request);
        User saved = userRepository.save(user);
        return toResponse(saved);
    }
    
    // Complex transaction with multiple operations
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        timeout = 30,
        rollbackFor = {Exception.class}
    )
    public OrderResponse createOrderForUser(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InactiveUserException("Cannot create order for inactive user");
        }
        
        Order order = Order.builder()
            .user(user)
            .items(request.getItems())
            .total(calculateTotal(request.getItems()))
            .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // Update user's last order date
        user.setLastOrderDate(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Order created for user {}: {}", userId, savedOrder.getId());
        
        return toOrderResponse(savedOrder);
    }
    
    // Programmatic transaction management for fine-grained control
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processUserBatch(List<CreateUserRequest> requests) {
        for (CreateUserRequest request : requests) {
            try {
                createUser(request);
            } catch (Exception e) {
                log.error("Failed to create user: {}", request.getEmail(), e);
                // Continue processing other users
            }
        }
    }
}

// ❌ Bad - Poor transaction management
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // ❌ No @Transactional annotation
    public void updateUser(User user) {
        userRepository.save(user);
    }
    
    // ❌ Inappropriate isolation level
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
```

## Database Migration (Flyway)

```sql
-- ✅ Good - V1__create_users_table.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    phone_number VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_status ON users(status);
CREATE INDEX idx_user_created_at ON users(created_at);

-- ✅ Good - V2__add_user_department.sql
ALTER TABLE users ADD COLUMN department_id BIGINT;

ALTER TABLE users 
    ADD CONSTRAINT fk_user_department 
    FOREIGN KEY (department_id) 
    REFERENCES departments(id) 
    ON DELETE SET NULL;

CREATE INDEX idx_user_department ON users(department_id);

-- ✅ Good - V3__create_user_roles_junction.sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_role_user_id ON user_roles(user_id);
CREATE INDEX idx_user_role_role_id ON user_roles(role_id);
```

## Query Optimization

```java
// ✅ Good - Optimized queries with projections
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Use projection interface for read-only data
    interface UserSummary {
        Long getId();
        String getName();
        String getEmail();
        UserStatus getStatus();
    }
    
    // Returns only needed fields
    List<UserSummary> findAllProjectedBy();
    
    Page<UserSummary> findByStatus(UserStatus status, Pageable pageable);
    
    // DTO projection with constructor expression
    @Query("SELECT new com.example.dto.UserDTO(u.id, u.name, u.email, d.name) " +
           "FROM User u LEFT JOIN u.department d WHERE u.status = :status")
    List<UserDTO> findUserDTOsByStatus(@Param("status") UserStatus status);
    
    // Efficient count query
    @Query("SELECT COUNT(u.id) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") UserStatus status);
    
    // Batch fetching to avoid N+1
    @EntityGraph(attributePaths = {"department", "orders"})
    List<User> findAll();
    
    // Stream for large result sets
    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "50"))
    @Query("SELECT u FROM User u WHERE u.status = :status")
    Stream<User> streamByStatus(@Param("status") UserStatus status);
}

// Usage of streaming
@Service
@Transactional(readOnly = true)
public class UserExportService {
    
    @Autowired
    private UserRepository userRepository;
    
    public void exportUsers(OutputStream outputStream) {
        try (Stream<User> userStream = userRepository.streamByStatus(UserStatus.ACTIVE)) {
            userStream.forEach(user -> {
                // Process each user without loading all into memory
                writeToOutputStream(user, outputStream);
            });
        }
    }
}

// ❌ Bad - Inefficient queries
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ❌ Returns full entities when only summary needed
    List<User> findAll();
    
    // ❌ No pagination
    @Query("SELECT u FROM User u JOIN u.orders o")
    List<User> findUsersWithOrders();  // ❌ N+1 problem
}
```

## Auditing Configuration

```java
// ✅ Good - JPA Auditing configuration
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }
}

@Component
public class AuditorAwareImpl implements AuditorAware<String> {
    
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName);
    }
}

// Auditable base entity
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
public abstract class AuditableEntity {
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
}

// Entity extending auditable base
@Entity
@Table(name = "users")
public class User extends AuditableEntity {
    // Entity fields...
}
```

## Connection Pool Configuration

```yaml
# ✅ Good - application.yml with optimized connection pool
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      auto-commit: false
      pool-name: SpringBootHikariCP
      
  jpa:
    hibernate:
      ddl-auto: validate  # Never use 'update' or 'create-drop' in production
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true
        query:
          in_clause_parameter_padding: true
        cache:
          use_second_level_cache: false
          use_query_cache: false
    show-sql: false
    open-in-view: false  # Disable to avoid lazy loading issues
```
````