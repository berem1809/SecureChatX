package com.chatapp.config;

// ============================================================================
// IMPORTS
// ============================================================================

// Our custom classes
import com.chatapp.repository.UserRepository;           // Database access for users
import com.chatapp.security.CustomUserDetailsService;   // Loads user details for Spring Security
import com.chatapp.security.JwtAuthenticationFilter;    // Our JWT validation filter
import com.chatapp.security.JwtTokenProvider;           // JWT token operations

// Spring Boot conditional annotations (not used here but imported)
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

// Spring Framework core annotations
import org.springframework.context.annotation.Bean;          // Marks methods that create Spring beans
import org.springframework.context.annotation.Configuration; // Marks this as a configuration class

// Spring Security imports
import org.springframework.security.authentication.AuthenticationManager;  // Handles authentication
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Enables @PreAuthorize etc.
import org.springframework.security.config.annotation.web.builders.HttpSecurity;  // Configures HTTP security
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // Enables Spring Security
import org.springframework.security.config.http.SessionCreationPolicy;  // Session management options
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Password hashing algorithm
import org.springframework.security.crypto.password.PasswordEncoder;     // Interface for password encoding
import org.springframework.security.web.SecurityFilterChain;             // The security filter chain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Default login filter

/**
 * ============================================================================
 * SECURITY CONFIGURATION - The heart of application security
 * ============================================================================
 * 
 * This class configures how Spring Security protects our application.
 * It's one of the most important configuration files in any Spring Boot app.
 * 
 * WHAT IS SPRING SECURITY?
 * ------------------------
 * Spring Security is a framework that handles:
 * - AUTHENTICATION: "Who are you?" (verifying identity)
 * - AUTHORIZATION: "What can you do?" (verifying permissions)
 * - PROTECTION: Against common attacks (CSRF, XSS, etc.)
 * 
 * HOW REQUESTS ARE PROCESSED:
 * ---------------------------
 * Every HTTP request goes through a chain of "filters" before reaching your controller:
 * 
 *   Client Request
 *        ↓
 *   [Filter 1: CORS]           → Handles cross-origin requests
 *        ↓
 *   [Filter 2: CSRF]           → Protects against CSRF attacks (disabled for APIs)
 *        ↓
 *   [Filter 3: Rate Limit]     → Our custom filter to prevent abuse
 *        ↓
 *   [Filter 4: JWT Auth]       → Our custom filter to validate JWT tokens ← WE ADD THIS
 *        ↓
 *   [Filter 5: Authorization]  → Checks if user can access the endpoint
 *        ↓
 *   Your Controller
 * 
 * KEY SECURITY DECISIONS IN THIS CONFIG:
 * --------------------------------------
 * 1. STATELESS SESSIONS: No server-side sessions (JWT contains all info)
 * 2. CSRF DISABLED: Safe for APIs that use JWT (no cookies for auth)
 * 3. PUBLIC ENDPOINTS: /api/auth/** accessible without login
 * 4. PROTECTED ENDPOINTS: Everything else requires valid JWT
 * 5. BCRYPT PASSWORD HASHING: Industry-standard secure hashing
 * 
 * ANNOTATIONS EXPLAINED:
 * ----------------------
 * @Configuration: Tells Spring this class contains bean definitions
 * @EnableWebSecurity: Enables Spring Security's web security features
 * @EnableMethodSecurity: Allows @PreAuthorize, @PostAuthorize on methods
 * 
 * @see JwtAuthenticationFilter Our custom filter that validates JWTs
 * @see JwtTokenProvider Generates and validates JWT tokens
 */
@Configuration       // This class contains Spring configuration
@EnableWebSecurity   // Enable Spring Security for web applications
@EnableMethodSecurity // Enable method-level security annotations like @PreAuthorize
public class SecurityConfig {
    
    // ========================================================================
    // DEPENDENCIES - Injected by Spring (Constructor Injection)
    // ========================================================================
    
    /**
     * JwtTokenProvider handles JWT operations (generate, validate, parse).
     * We need it to create our JwtAuthenticationFilter.
     */
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * UserRepository provides database access to user data.
     * The JWT filter uses this to load user details after validating a token.
     */
    private final UserRepository userRepository;
    
    /**
     * CustomUserDetailsService loads user data for Spring Security.
     * Used by Spring's authentication mechanisms internally.
     */
    private final CustomUserDetailsService userDetailsService;

    /**
     * Constructor Injection - Spring automatically provides these dependencies.
     * 
     * WHY CONSTRUCTOR INJECTION?
     * - Dependencies are required (not optional)
     * - Fields can be 'final' (immutable after construction)
     * - Easier to test (can pass mocks in tests)
     * - Makes dependencies explicit and visible
     * 
     * @param jwtTokenProvider For JWT operations
     * @param userRepository For database access
     * @param userDetailsService For loading user details
     */
    public SecurityConfig(JwtTokenProvider jwtTokenProvider, 
                          UserRepository userRepository, 
                          CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    // ========================================================================
    // SECURITY FILTER CHAIN - Main security configuration
    // ========================================================================
    
    /**
     * Configures the security filter chain - THE MAIN SECURITY CONFIGURATION.
     * 
     * This method defines:
     * - Which endpoints are public vs protected
     * - How authentication works (JWT)
     * - Session management (stateless)
     * - CORS and CSRF settings
     * 
     * @Bean annotation means Spring will manage this object and inject it where needed.
     * 
     * @param http HttpSecurity builder to configure security
     * @return Configured SecurityFilterChain
     * @throws Exception If configuration fails
     */
    @Bean  // This method creates a Spring-managed bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        // Create our custom JWT filter that will validate tokens on every request
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository);

        return http
            // ================================================================
            // CSRF PROTECTION - Disabled for stateless API
            // ================================================================
            // CSRF (Cross-Site Request Forgery) protection is for browser-based
            // apps that use cookies for authentication.
            // 
            // WHY DISABLE?
            // - Our API uses JWT tokens in headers, not cookies
            // - CSRF attacks exploit automatic cookie sending
            // - JWT in Authorization header is immune to CSRF
            // - Keeping CSRF enabled would require sending CSRF tokens with every request
            .csrf(csrf -> csrf.disable())
            
            // ================================================================
            // CORS - Cross-Origin Resource Sharing
            // ================================================================
            // Allows our frontend (on different port/domain) to call our API.
            // Detailed CORS config is in WebMvcConfig.java
            .cors(cors -> cors.configure(http))
            
            // ================================================================
            // SESSION MANAGEMENT - Stateless (no server-side sessions)
            // ================================================================
            // STATELESS means:
            // - Server doesn't store any session data
            // - Each request must include authentication (JWT)
            // - No "Remember Me" using sessions
            // - Scales better (no session replication needed)
            // 
            // OTHER OPTIONS:
            // - ALWAYS: Always create a session
            // - IF_REQUIRED: Create session only if needed (default)
            // - NEVER: Never create, but use if exists
            // - STATELESS: Never create or use sessions ← We use this
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // ================================================================
            // AUTHORIZATION RULES - Who can access what
            // ================================================================
            // This is where we define which endpoints are public vs protected.
            // Rules are evaluated in ORDER - first match wins!
            .authorizeHttpRequests(auth -> auth
                // PUBLIC ENDPOINTS: No authentication required
                // "/api/auth/**" matches:
                //   - /api/auth/register
                //   - /api/auth/login
                //   - /api/auth/verify
                //   - /api/auth/refresh
                //   - /api/auth/logout
                //   - /api/auth/anything/else
                .requestMatchers("/api/auth/**").permitAll()
                
                // PROTECTED ENDPOINTS: Must be authenticated
                // anyRequest() = all other requests not matched above
                // authenticated() = user must have valid JWT
                .anyRequest().authenticated()
            )
            
            // ================================================================
            // ADD OUR JWT FILTER - Before Spring's default username/password filter
            // ================================================================
            // addFilterBefore() inserts our filter into the chain.
            // We put it BEFORE UsernamePasswordAuthenticationFilter because:
            // - JWT auth should happen before any form-based auth
            // - We want to authenticate EARLY in the chain
            // - If JWT is valid, no need to check username/password
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Build and return the configured SecurityFilterChain
            .build();
    }

    // ========================================================================
    // PASSWORD ENCODER - BCrypt for secure password hashing
    // ========================================================================
    
    /**
     * Creates a BCrypt password encoder for hashing passwords.
     * 
     * WHAT IS PASSWORD HASHING?
     * -------------------------
     * Hashing converts a password into an unreadable string:
     * "MyPassword123" → "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
     * 
     * WHY BCRYPT?
     * -----------
     * BCrypt is designed specifically for passwords:
     * 
     * 1. SALT: Adds random data to each password before hashing
     *    - Same password → Different hash each time
     *    - Prevents rainbow table attacks
     * 
     * 2. SLOW BY DESIGN: Takes ~100ms to hash
     *    - Normal for 1 login, but 10 million guesses = 11 days
     *    - Can adjust "strength" (cost factor) as computers get faster
     * 
     * 3. ADAPTIVE: Cost factor can be increased over time
     *    - Default strength is 10 (2^10 = 1024 rounds)
     *    - Increase to 11, 12, etc. as hardware improves
     * 
     * COMPARISON WITH OTHER ALGORITHMS:
     * | Algorithm | Speed     | Security | Use Case |
     * |-----------|-----------|----------|----------|
     * | MD5       | Very Fast | Broken   | NEVER use for passwords |
     * | SHA-256   | Fast      | OK       | Data integrity, not passwords |
     * | BCrypt    | Slow      | Excellent| Passwords ✓ |
     * | Argon2    | Slow      | Best     | Passwords (newer) |
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean  // Spring manages this bean - inject it anywhere with @Autowired
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder with default strength (10)
        // Strength 10 means 2^10 = 1024 hashing rounds
        return new BCryptPasswordEncoder();
    }

    // ========================================================================
    // AUTHENTICATION MANAGER - Handles authentication requests
    // ========================================================================
    
    /**
     * Creates an AuthenticationManager from Spring's auto-configuration.
     * 
     * WHAT IS AUTHENTICATION MANAGER?
     * -------------------------------
     * AuthenticationManager is the main interface for authentication in Spring Security.
     * It takes an Authentication request and returns an authenticated principal.
     * 
     * WHEN IS IT USED?
     * - When we need to manually authenticate (e.g., in login endpoint)
     * - When using @PreAuthorize with authentication checks
     * - When other parts of the app need to verify credentials
     * 
     * WHY THIS APPROACH?
     * - Spring Boot auto-configures an AuthenticationManager
     * - We just expose it as a bean so we can inject it elsewhere
     * - The actual configuration comes from AuthenticationConfiguration
     * 
     * @param configuration Spring's AuthenticationConfiguration
     * @return AuthenticationManager instance
     * @throws Exception If unable to create AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // Get the auto-configured AuthenticationManager from Spring
        return configuration.getAuthenticationManager();
    }
}