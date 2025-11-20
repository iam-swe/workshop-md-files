````instructions
---
description: 'Java Spring Boot security standards - Authentication, Authorization, and Security best practices'
applyTo: '**/security/**/*.java, **/config/Security*.java, **/filter/**/*.java'
---

# Java Security Standards

## Core Security Principles (Non-Negotiable)

- **NEVER store passwords in plain text** - Always use BCrypt, Argon2, or PBKDF2
- **NEVER expose sensitive data in logs** - Mask passwords, tokens, and PII
- **NEVER trust user input** - Always validate and sanitize
- **ALWAYS use HTTPS in production** - No exceptions
- **ALWAYS implement CSRF protection** for state-changing operations
- **NEVER hardcode secrets** - Use environment variables or secret managers
- **ALWAYS implement rate limiting** - Prevent brute force attacks
- **NEVER return detailed error messages** to clients - Log details server-side only

## Spring Security Configuration

```java
// ✅ Good - Comprehensive security configuration
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/v1/auth/**")
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none';")
                )
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .frameOptions(FrameOptionsConfig::deny)
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "https://yourdomain.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Cost factor of 12
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
            throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }
}

// ❌ Bad - Insecure configuration
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()  // ❌ CSRF disabled globally
            .authorizeRequests()
            .anyRequest().permitAll();  // ❌ Everything accessible
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();  // ❌ No password encoding
    }
}
```

## JWT Authentication

```java
// ✅ Good - Secure JWT implementation
@Service
@RequiredArgsConstructor
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration:3600000}") // 1 hour default
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private long refreshExpiration;
    
    private static final String AUTHORITIES_CLAIM = "authorities";
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(AUTHORITIES_CLAIM, userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        
        return buildToken(claims, userDetails.getUsername(), jwtExpiration);
    }
    
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails.getUsername(), refreshExpiration);
    }
    
    private String buildToken(
            Map<String, Object> claims,
            String subject,
            long expiration
    ) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }
    
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

// JWT Authentication Filter
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}

// ❌ Bad - Insecure JWT implementation
@Service
public class JwtService {
    
    private String secretKey = "mySecretKey123";  // ❌ Hardcoded secret
    
    public String generateToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .signWith(SignatureAlgorithm.HS256, secretKey)  // ❌ Weak algorithm
            .compact();  // ❌ No expiration
    }
    
    public String extractUsername(String token) {
        return Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();  // ❌ No error handling
    }
}
```

## Authentication Service

```java
// ✅ Good - Secure authentication service
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        
        // Validate password strength
        validatePasswordStrength(request.getPassword());
        
        // Create user with encoded password
        User user = User.builder()
            .email(request.getEmail())
            .name(request.getName())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.USER)
            .enabled(false) // Require email verification
            .accountNonLocked(true)
            .build();
        
        User savedUser = userRepository.save(user);
        
        // Generate verification token
        String verificationToken = generateVerificationToken(savedUser);
        
        // Send verification email (async)
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
        
        log.info("User registered successfully: {}", savedUser.getEmail());
        
        return AuthenticationResponse.builder()
            .message("Registration successful. Please verify your email.")
            .build();
    }
    
    public AuthenticationResponse login(LoginRequest request) {
        String email = request.getEmail();
        
        // Check if account is locked due to failed attempts
        if (loginAttemptService.isBlocked(email)) {
            throw new AccountLockedException("Account temporarily locked due to multiple failed login attempts");
        }
        
        try {
            // Authenticate user
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    email,
                    request.getPassword()
                )
            );
            
            // Reset failed login attempts
            loginAttemptService.loginSucceeded(email);
            
            // Load user details
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // Check if email is verified
            if (!user.isEnabled()) {
                throw new EmailNotVerifiedException("Please verify your email before logging in");
            }
            
            // Generate tokens
            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            // Save refresh token
            saveRefreshToken(user, refreshToken);
            
            log.info("User logged in successfully: {}", email);
            
            return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
            
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(email);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }
    
    public AuthenticationResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !refreshToken.startsWith("Bearer ")) {
            throw new InvalidTokenException("Invalid refresh token format");
        }
        
        String token = refreshToken.substring(7);
        String username = jwtService.extractUsername(token);
        
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        if (!jwtService.isTokenValid(token, user)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        
        // Verify refresh token exists in database
        if (!isRefreshTokenValid(user, token)) {
            throw new InvalidTokenException("Refresh token not found or revoked");
        }
        
        String newAccessToken = jwtService.generateToken(user);
        
        return AuthenticationResponse.builder()
            .accessToken(newAccessToken)
            .tokenType("Bearer")
            .expiresIn(3600L)
            .build();
    }
    
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Revoke all refresh tokens
        revokeAllUserTokens(user);
        
        log.info("User logged out: {}", email);
    }
    
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters long");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new WeakPasswordException("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new WeakPasswordException("Password must contain at least one digit");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new WeakPasswordException("Password must contain at least one special character");
        }
    }
}

// ❌ Bad - Insecure authentication
@Service
public class AuthenticationService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && user.getPassword().equals(password)) {  // ❌ Plain text comparison
            return user;
        }
        throw new RuntimeException("Invalid credentials");  // ❌ Reveals info about user existence
    }
}
```

## Rate Limiting

```java
// ✅ Good - Rate limiting implementation
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String clientId = getClientId(request);
        RateLimiter rateLimiter = limiters.computeIfAbsent(
            clientId,
            k -> RateLimiter.create(100.0) // 100 requests per second
        );
        
        if (!rateLimiter.tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
            log.warn("Rate limit exceeded for client: {}", clientId);
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getClientId(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }
}

// Login attempt tracking
@Service
@Slf4j
public class LoginAttemptService {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 15;
    
    private final LoadingCache<String, Integer> attemptsCache;
    
    public LoginAttemptService() {
        attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(LOCK_TIME_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Integer>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });
    }
    
    public void loginSucceeded(String email) {
        attemptsCache.invalidate(email);
    }
    
    public void loginFailed(String email) {
        int attempts = attemptsCache.getUnchecked(email);
        attempts++;
        attemptsCache.put(email, attempts);
        
        if (attempts >= MAX_ATTEMPTS) {
            log.warn("Account locked due to {} failed login attempts: {}", attempts, email);
        }
    }
    
    public boolean isBlocked(String email) {
        try {
            return attemptsCache.get(email) >= MAX_ATTEMPTS;
        } catch (ExecutionException e) {
            return false;
        }
    }
}
```

## Input Validation and Sanitization

```java
// ✅ Good - Input validation
@Component
public class InputValidator {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*(\\bOR\\b|\\bAND\\b|\\bUNION\\b|\\bSELECT\\b|\\bDROP\\b|\\bINSERT\\b|\\bUPDATE\\b|\\bDELETE\\b).*",
        Pattern.CASE_INSENSITIVE
    );
    
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove potentially dangerous characters
        String sanitized = input.replaceAll("[<>\"'%;()&+]", "");
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(sanitized).matches()) {
            throw new InvalidInputException("Input contains potentially malicious content");
        }
        
        return sanitized.trim();
    }
    
    public void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailException("Invalid email format");
        }
    }
    
    public void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be blank");
        }
    }
}
```

## Method-Level Security

```java
// ✅ Good - Method-level security annotations
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }
    
    @PreAuthorize("hasRole('ADMIN') or #email == authentication.principal.username")
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .map(this::toResponse)
            .orElseThrow(() -> new UserNotFoundException(email));
    }
    
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        
        updateUserFromRequest(user, request);
        User updated = userRepository.save(user);
        
        return toResponse(updated);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostAuthorize("returnObject.email != 'admin@system.com'")
    public UserResponse deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        
        UserResponse response = toResponse(user);
        userRepository.delete(user);
        
        return response;
    }
}
```

## Sensitive Data Handling

```java
// ✅ Good - Masking sensitive data in logs
@Aspect
@Component
@Slf4j
public class SensitiveDataMaskingAspect {
    
    @Around("@annotation(com.example.annotation.MaskSensitiveData)")
    public Object maskSensitiveData(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Object[] maskedArgs = Arrays.stream(args)
            .map(this::maskObject)
            .toArray();
        
        log.info("Method: {}, Args: {}", joinPoint.getSignature().getName(), maskedArgs);
        
        return joinPoint.proceed();
    }
    
    private Object maskObject(Object obj) {
        if (obj instanceof String str) {
            if (str.contains("password") || str.contains("token")) {
                return "***MASKED***";
            }
        }
        return obj;
    }
}

// Custom JSON serializer to mask sensitive fields
public class SensitiveDataSerializer extends JsonSerializer<String> {
    
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (value != null && value.length() > 4) {
            gen.writeString("****" + value.substring(value.length() - 4));
        } else {
            gen.writeString("****");
        }
    }
}

// Usage in DTO
@Data
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    
    @JsonSerialize(using = SensitiveDataSerializer.class)
    private String phoneNumber;
    
    @JsonIgnore
    private String password;  // Never serialize passwords
}
```

## Security Headers

```java
// ✅ Good - Security headers configuration
@Configuration
public class SecurityHeadersConfig {
    
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
}

public class SecurityHeadersFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Prevent XSS attacks
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Content Security Policy
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;");
        
        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=()");
        
        // Strict Transport Security (HSTS)
        response.setHeader("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains; preload");
        
        filterChain.doFilter(request, response);
    }
}
```
````