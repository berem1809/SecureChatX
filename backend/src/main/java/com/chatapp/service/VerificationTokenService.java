package com.chatapp.service;

// ============================================================================
// IMPORTS
// ============================================================================

// Logging framework (SLF4J with Logback implementation)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Spring's Redis template for String operations
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.stereotype.Service;  // Marks this as a Spring service

import java.time.Duration;  // For TTL (time-to-live)
import java.util.Optional;  // Container for optional values
import java.util.UUID;      // For generating unique tokens

/**
 * ============================================================================
 * VERIFICATION TOKEN SERVICE - Manages email verification tokens in Redis
 * ============================================================================
 * 
 * WHAT IS A VERIFICATION TOKEN?
 * -----------------------------
 * When a user registers, we need to confirm their email is real.
 * We generate a unique token, send it via email, and wait for them to click.
 * 
 * FLOW:
 * 1. User registers with email "john@example.com"
 * 2. We generate UUID: "550e8400-e29b-41d4-a716-446655440000"
 * 3. Store in Redis: token → userId mapping
 * 4. Send email with link: /verify?token=550e8400-e29b-41d4-a716-446655440000
 * 5. User clicks link
 * 6. We look up token in Redis → get userId
 * 7. Update user status to "ACTIVE"
 * 8. Delete token from Redis
 * 
 * WHY USE UUID?
 * -------------
 * - Universally Unique Identifier - 128 bits of randomness
 * - Impossible to guess (2^128 possible values)
 * - No need to hash (unlike refresh tokens) because:
 *   a) Single use - deleted after verification
 *   b) Short lived - 24 hours only
 *   c) Low value target - can only verify email, not access account
 * 
 * REDIS STORAGE STRATEGY:
 * -----------------------
 * We store TWO key-value pairs per token for bi-directional lookup:
 * 
 * 1. Token → User lookup (for verification):
 *    Key:   "verification:token:{uuid}"
 *    Value: userId (as string)
 *    
 * 2. User → Token lookup (to delete old token if user re-registers):
 *    Key:   "verification:user:{userId}"
 *    Value: uuid
 * 
 * EXAMPLE:
 * --------
 * Token: "550e8400-e29b-41d4-a716-446655440000"
 * userId: 42
 * 
 * Stored keys:
 * - "verification:token:550e8400-e29b-41d4..." → "42"
 * - "verification:user:42" → "550e8400-e29b-41d4..."
 * 
 * TTL (Time-To-Live):
 * -------------------
 * Both keys expire after 24 hours (TOKEN_TTL).
 * If user doesn't verify in 24 hours:
 * - Token auto-deletes from Redis
 * - User must request new verification email
 * 
 * @see AuthService Where verification tokens are created/validated
 * @see EmailService Where verification emails are sent
 */
@Service  // Marks this as a Spring-managed service bean
public class VerificationTokenService {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Logger for debugging and auditing */
    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenService.class);
    
    /** Prefix for token → userId lookup. Example: "verification:token:550e8400..." */
    private static final String TOKEN_KEY_PREFIX = "verification:token:";
    
    /** Prefix for userId → token lookup. Example: "verification:user:42" */
    private static final String USER_KEY_PREFIX = "verification:user:";
    
    /** How long verification tokens live in Redis (24 hours) */
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    /**
     * StringRedisTemplate - Spring's helper for Redis String operations.
     * Configured in RedisConfig class with connection to Redis server.
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor injection - Spring provides the Redis template.
     */
    public VerificationTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========================================================================
    // CREATE TOKEN
    // ========================================================================

    /**
     * Creates a verification token for a user and stores it in Redis.
     * 
     * STEPS:
     * 1. Delete any existing token for this user
     * 2. Generate new UUID token
     * 3. Store token → userId mapping
     * 4. Store userId → token mapping
     * 5. Set TTL on both keys (24 hours)
     * 6. Return token for email link
     * 
     * @param userId The user's MySQL ID
     * @return The generated UUID token (to include in verification email)
     */
    public String createVerificationToken(Long userId) {
        String userIdStr = userId.toString();
        
        // Delete any existing token for this user (if they re-register)
        deleteByUserId(userId);
        
        // Generate random UUID token
        // Example: "550e8400-e29b-41d4-a716-446655440000"
        String token = UUID.randomUUID().toString();
        
        // Build Redis keys
        String tokenKey = TOKEN_KEY_PREFIX + token;    // "verification:token:550e8400..."
        String userKey = USER_KEY_PREFIX + userIdStr;  // "verification:user:42"
        
        // Store both mappings with TTL (24 hours)
        redisTemplate.opsForValue().set(tokenKey, userIdStr, TOKEN_TTL);
        redisTemplate.opsForValue().set(userKey, token, TOKEN_TTL);
        
        logger.info("Created verification token for userId: {} with TTL: {} hours", userId, TOKEN_TTL.toHours());
        return token;  // Return to caller for email link
    }

    // ========================================================================
    // VALIDATE / FIND TOKEN
    // ========================================================================

    /**
     * Finds the userId associated with a verification token.
     * Called when user clicks the verification link.
     * 
     * STEPS:
     * 1. Build the key: "verification:token:{token}"
     * 2. Query Redis for userId
     * 3. Return Optional.of(userId) or Optional.empty()
     * 
     * @param token The verification token from email link
     * @return Optional containing userId if valid, empty if invalid/expired
     */
    public Optional<Long> findUserIdByToken(String token) {
        String tokenKey = TOKEN_KEY_PREFIX + token;
        
        // Query Redis - returns null if key doesn't exist or expired
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        
        if (userIdStr != null) {
            return Optional.of(Long.parseLong(userIdStr));
        }
        return Optional.empty();  // Token not found or expired
    }

    // ========================================================================
    // DELETE TOKEN
    // ========================================================================

    /**
     * Deletes verification token by token string.
     * Called after successful email verification (token is single-use).
     * 
     * STEPS:
     * 1. Look up userId from token
     * 2. Delete user → token mapping
     * 3. Delete token → userId mapping
     * 
     * @param token The token to delete
     */
    public void deleteByToken(String token) {
        String tokenKey = TOKEN_KEY_PREFIX + token;
        
        // Get userId so we can delete the reverse mapping too
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        
        if (userIdStr != null) {
            String userKey = USER_KEY_PREFIX + userIdStr;
            redisTemplate.delete(userKey);  // Delete user → token mapping
        }
        redisTemplate.delete(tokenKey);  // Delete token → userId mapping
        
        logger.debug("Deleted verification token: {}", token);
    }

    /**
     * Deletes verification token by userId.
     * Used when:
     * - Creating new token (delete old one first)
     * - Admin cancels pending verification
     * 
     * @param userId The user ID whose token should be deleted
     */
    public void deleteByUserId(Long userId) {
        String userIdStr = userId.toString();
        String userKey = USER_KEY_PREFIX + userIdStr;
        
        // Get the token so we can delete the token key too
        String token = redisTemplate.opsForValue().get(userKey);
        
        if (token != null) {
            String tokenKey = TOKEN_KEY_PREFIX + token;
            redisTemplate.delete(tokenKey);  // Delete token → userId mapping
        }
        redisTemplate.delete(userKey);  // Delete user → token mapping
        
        logger.debug("Deleted verification token for userId: {}", userId);
    }

    // ========================================================================
    // CHECK TOKEN
    // ========================================================================

    /**
     * Checks if a verification token exists and is valid (not expired).
     * 
     * Useful for checking before processing verification.
     * 
     * @param token The token to check
     * @return true if token exists in Redis, false otherwise
     */
    public boolean isValidToken(String token) {
        String tokenKey = TOKEN_KEY_PREFIX + token;
        
        // hasKey returns Boolean, could be null, so we check for TRUE
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }
}
