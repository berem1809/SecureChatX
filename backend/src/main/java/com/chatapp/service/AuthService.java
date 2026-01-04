package com.chatapp.service;

// ============================================================================
// IMPORTS
// ============================================================================

// DTOs (Data Transfer Objects) for request/response
import com.chatapp.dto.AuthResponse;     // Response with access + refresh tokens
import com.chatapp.dto.LoginRequest;     // Login form data
import com.chatapp.dto.RegisterRequest;  // Registration form data

// Model and exceptions
import com.chatapp.model.User;  // User entity for MySQL
import com.chatapp.exception.EmailAlreadyExistsException;  // Thrown when email exists
import com.chatapp.exception.InvalidTokenException;  // Thrown for bad tokens

// Repository and security
import com.chatapp.repository.UserRepository;  // MySQL database access
import com.chatapp.security.JwtTokenProvider;  // JWT token generation

// Logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Spring components
import org.springframework.security.crypto.password.PasswordEncoder;  // BCrypt encoder
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * AUTH SERVICE INTERFACE - Contract for authentication operations
 * ============================================================================
 * 
 * WHY USE AN INTERFACE?
 * ---------------------
 * 1. Abstraction: Hides implementation details from callers
 * 2. Testability: Easy to mock in unit tests
 * 3. Flexibility: Could swap implementations (e.g., LDAP instead of database)
 * 4. Spring Best Practice: Program to interfaces, not implementations
 * 
 * This interface defines WHAT authentication operations are available.
 * The AuthServiceImpl class defines HOW they work.
 */
public interface AuthService {
    
    /**
     * Registers a new user account.
     * Creates user in MySQL, sends verification email.
     * 
     * @param request Contains email, password, displayName
     * @throws EmailAlreadyExistsException if email is already registered
     */
    void register(RegisterRequest request);
    
    /**
     * Authenticates user and returns JWT tokens.
     * 
     * @param request Contains email and password
     * @return AuthResponse with access token and refresh token
     * @throws RuntimeException if credentials invalid or email not verified
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * Verifies user's email address using token from verification email.
     * 
     * @param token The verification token from email link
     * @throws InvalidTokenException if token is invalid or expired
     */
    void verifyEmail(String token);
    
    /**
     * Refreshes access token using a valid refresh token.
     * 
     * @param refreshToken The refresh token
     * @return AuthResponse with new access token (same refresh token)
     * @throws InvalidTokenException if refresh token is invalid or expired
     */
    AuthResponse refresh(String refreshToken);
    
    /**
     * Logs out user by invalidating their refresh token.
     * 
     * @param refreshToken The refresh token to invalidate
     */
    void logout(String refreshToken);
}

/**
 * ============================================================================
 * AUTH SERVICE IMPLEMENTATION - Core authentication logic
 * ============================================================================
 * 
 * This class implements all authentication operations:
 * - User registration with email verification
 * - Login with JWT token generation
 * - Token refresh for session extension
 * - Logout with token invalidation
 * 
 * ARCHITECTURE - Two-Database Design:
 * -----------------------------------
 * 
 * MYSQL (Permanent Storage):
 * - User accounts (email, password hash, status, roles)
 * - Persistent data that never auto-expires
 * - Managed by JPA/Hibernate via UserRepository
 * 
 * REDIS (Temporary Storage):
 * - Verification tokens (24-hour TTL)
 * - Refresh tokens (30-day TTL)
 * - Auto-expires data with TTL
 * - Managed by StringRedisTemplate via token services
 * 
 * WHY THIS DESIGN?
 * ----------------
 * 1. MySQL: Reliable ACID storage for critical user data
 * 2. Redis: Fast, auto-expiring storage for temporary tokens
 * 3. Separation of concerns: Different data, different storage needs
 * 
 * SECURITY FEATURES:
 * ------------------
 * - Passwords hashed with BCrypt (10 rounds)
 * - Refresh tokens hashed with SHA-256 before storing in Redis
 * - Single-session design: new login invalidates old refresh token
 * - Generic error messages prevent user enumeration
 * 
 * @see AuthController The REST controller that uses this service
 */
@Service  // Marks this as a Spring-managed service bean
class AuthServiceImpl implements AuthService {

    // ========================================================================
    // LOGGER
    // ========================================================================
    
    /**
     * Logger for this class - used for tracking authentication events.
     * 
     * WHY LOG AUTHENTICATION EVENTS?
     * - Security auditing (who logged in when)
     * - Debugging (why did login fail?)
     * - Monitoring (track failed login attempts)
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    // ========================================================================
    // DEPENDENCIES (Injected by Spring)
    // ========================================================================
    
    private final UserRepository userRepository;           // MySQL access for users
    private final VerificationTokenService verificationTokenService;  // Redis verification tokens
    private final RefreshTokenService refreshTokenService;  // Redis refresh tokens
    private final EmailService emailService;               // Send verification emails
    private final PasswordEncoder passwordEncoder;         // BCrypt for password hashing
    private final JwtTokenProvider jwtTokenProvider;       // JWT generation/validation

    /**
     * Constructor injection - Spring provides all dependencies.
     * 
     * All dependencies are marked as 'final' because:
     * 1. They should never change after construction
     * 2. Makes the class thread-safe
     * 3. Clearly indicates required dependencies
     */
    public AuthServiceImpl(UserRepository userRepository,
                           VerificationTokenService verificationTokenService,
                           RefreshTokenService refreshTokenService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.verificationTokenService = verificationTokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }

    // ========================================================================
    // REGISTRATION
    // ========================================================================

    /**
     * Registers a new user in the system.
     * 
     * REGISTRATION FLOW:
     * ==================
     * 1. Check if email already exists → throw exception if yes
     * 2. Create User in MySQL with status "PENDING_VERIFICATION"
     * 3. Generate verification token in Redis (24-hour TTL)
     * 4. Send verification email with token link
     * 
     * USER CANNOT LOGIN until they verify email!
     * 
     * @param request Contains: email, password, displayName
     * @throws EmailAlreadyExistsException if email already registered
     */
    @Override
    public void register(RegisterRequest request) {
        logger.info("Register method called for: {}", request.getEmail());
        
        // STEP 1: Check for duplicate email
        // This prevents multiple accounts with same email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }
        
        // STEP 2: Create user in MySQL with PENDING_VERIFICATION status
        // Password is hashed with BCrypt before storing
        // No tokens created yet - user must verify email first
        User user = new User(
            request.getEmail(), 
            passwordEncoder.encode(request.getPassword()),  // BCrypt hash!
            request.getDisplayName(), 
            "PENDING_VERIFICATION",  // Cannot login until verified
            java.util.List.of("ROLE_USER")  // Default role for all users
        );
        user = userRepository.save(user);  // Persist to MySQL, gets auto-generated ID
        logger.info("User saved to MySQL with id: {}", user.getId());

        // STEP 3: Create verification token in Redis
        // Token is UUID, stored with 24-hour TTL
        String token = verificationTokenService.createVerificationToken(user.getId());
        logger.info("Verification token created in Redis for user: {}", user.getEmail());

        // STEP 4: Send verification email
        // Email contains link: /api/auth/verify?token={uuid}
        logger.info("Sending verification email to: {}", user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    // ========================================================================
    // EMAIL VERIFICATION
    // ========================================================================

    /**
     * Verifies user's email address.
     * 
     * VERIFICATION FLOW:
     * ==================
     * 1. Look up token in Redis → get userId
     * 2. Load user from MySQL by ID
     * 3. Update user status to "ACTIVE"
     * 4. Delete verification token from Redis
     * 
     * After this, user CAN LOGIN!
     * 
     * @param token The verification token from email link
     * @throws InvalidTokenException if token not found or expired
     */
    @Override
    public void verifyEmail(String token) {
        logger.info("Verify method called with token: {}", token);
        
        // STEP 1: Find userId from Redis verification token
        // Token auto-expires after 24 hours (Redis TTL)
        Long userId = verificationTokenService.findUserIdByToken(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification token"));
        
        // STEP 2: Load user from MySQL
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // STEP 3: Update status to ACTIVE
        user.setStatus("ACTIVE");
        userRepository.save(user);  // Persist status change
        
        // STEP 4: Delete token from Redis (one-time use)
        verificationTokenService.deleteByToken(token);
        
        logger.info("Email verified successfully for user: {}", user.getEmail());
    }

    // ========================================================================
    // LOGIN
    // ========================================================================

    /**
     * Authenticates user and returns JWT tokens.
     * 
     * LOGIN FLOW:
     * ===========
     * 1. Find user by email → fail if not found
     * 2. Verify password matches hash → fail if wrong
     * 3. Check status is ACTIVE → fail if not verified
     * 4. Generate access token (15 min) and refresh token (30 days)
     * 5. Store refresh token in Redis (hashed)
     * 6. Return both tokens to client
     * 
     * SECURITY NOTES:
     * - Generic error messages prevent user enumeration
     * - Old refresh tokens are deleted (single-session)
     * - Refresh token is hashed before Redis storage
     * 
     * @param request Contains: email, password
     * @return AuthResponse with accessToken and refreshToken
     * @throws RuntimeException if authentication fails
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        
        // STEP 1: Find user by email
        // Note: We log "email not found" but return generic error to user
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                logger.warn("SECURITY: Failed login attempt - email not found: {}", request.getEmail());
                return new RuntimeException("Invalid credentials");  // Generic message!
            });
        
        // STEP 2: Verify password
        // passwordEncoder.matches() compares plain text to BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("SECURITY: Failed login attempt - invalid password for user: {}", request.getEmail());
            throw new RuntimeException("Invalid credentials");  // Same generic message!
        }
        
        // STEP 3: Check email is verified
        if (!"ACTIVE".equals(user.getStatus())) {
            logger.warn("SECURITY: Login attempt for unverified email: {}", request.getEmail());
            throw new RuntimeException("Email not verified. Please verify your email before logging in.");
        }

        // STEP 4: Generate JWT tokens
        // Claims = extra data embedded in the token
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());  // Include roles for authorization
        
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), claims);   // 15 min
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());  // 30 days

        // STEP 5: Store refresh token in Redis
        // Delete any existing token first (single-session design)
        refreshTokenService.deleteByUserId(user.getId());
        refreshTokenService.createRefreshToken(refreshToken, user.getId());

        logger.info("User logged in: {}", user.getEmail());
        
        // STEP 6: Return tokens to client
        return new AuthResponse(accessToken, refreshToken);
    }

    // ========================================================================
    // TOKEN REFRESH
    // ========================================================================

    /**
     * Refreshes access token using valid refresh token.
     * 
     * REFRESH FLOW:
     * =============
     * 1. Validate JWT signature of refresh token
     * 2. Check token exists in Redis (not revoked)
     * 3. Load user from MySQL
     * 4. Generate new access token
     * 5. Return new access token + same refresh token
     * 
     * WHY NOT ROTATE REFRESH TOKEN?
     * In this simple implementation, we keep the same refresh token.
     * For higher security, you could implement "refresh token rotation":
     * - Generate new refresh token on each refresh
     * - Invalidate old refresh token
     * - Detect token reuse (possible theft)
     * 
     * @param refreshToken The refresh token from client
     * @return AuthResponse with new access token
     * @throws InvalidTokenException if refresh token is invalid
     */
    @Override
    public AuthResponse refresh(String refreshToken) {
        
        // STEP 1: Validate JWT signature
        // This checks the token wasn't tampered with
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        
        // STEP 2: Verify token exists in Redis
        // Even if JWT is valid, token could be revoked (logged out)
        Long userId = refreshTokenService.findUserIdByToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found or expired"));
        
        // STEP 3: Load user from MySQL
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // STEP 4: Generate new access token with fresh claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), claims);
        
        // STEP 5: Return new access token + same refresh token
        return new AuthResponse(accessToken, refreshToken);
    }

    // ========================================================================
    // LOGOUT
    // ========================================================================

    /**
     * Logs out user by invalidating their refresh token.
     * 
     * LOGOUT FLOW:
     * ============
     * 1. Delete refresh token from Redis
     * 2. Client should discard both tokens locally
     * 
     * NOTE: Access token will still be valid until it expires (15 min).
     * This is a trade-off of JWT - tokens can't be revoked individually.
     * For immediate invalidation, you'd need a token blacklist (more complex).
     * 
     * @param refreshToken The refresh token to invalidate
     */
    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
            logger.info("Refresh token deleted on logout");
        }
    }
}
