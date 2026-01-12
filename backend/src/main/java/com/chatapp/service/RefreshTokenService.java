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

// Security imports for token hashing
import java.nio.charset.StandardCharsets;  // UTF-8 encoding
import java.security.MessageDigest;         // SHA-256 hashing
import java.security.NoSuchAlgorithmException;  // Hash algorithm not found

import java.time.Duration;  // For TTL (time-to-live)
import java.util.Base64;    // Base64 encoding for hash output
import java.util.Optional;  // Container for optional values

/**
 * ============================================================================
 * REFRESH TOKEN SERVICE - Manages refresh tokens in Redis
 * ============================================================================
 * 
 * WHAT IS A REFRESH TOKEN?
 * ------------------------
 * Refresh tokens allow users to get new access tokens without logging in again.
 * 
 * ACCESS TOKEN: Short-lived (15 min), used for API requests
 *   → Must be frequently renewed
 * 
 * REFRESH TOKEN: Long-lived (30 days), used ONLY to get new access tokens
 *   → Stored securely, used sparingly
 * 
 * WHY TWO TOKENS?
 * ---------------
 * - If access token is stolen, damage is limited (15 min)
 * - Refresh token is used less often, reducing theft risk
 * - User doesn't need to re-login every 15 minutes
 * 
 * REDIS STORAGE STRATEGY:
 * -----------------------
 * We store TWO key-value pairs per token for bi-directional lookup:
 * 
 * 1. Token → User lookup (for validation):
 *    Key:   "refresh:hash:{SHA256-of-token}"
 *    Value: userId (as string)
 *    
 * 2. User → Token lookup (for deletion on new login):
 *    Key:   "refresh:user:{userId}"
 *    Value: SHA256-of-token
 * 
 * EXAMPLE:
 * --------
 * Token: "eyJhbGciOiJIUzUxMi..." 
 * SHA256: "abc123..." (hashed version)
 * userId: 42
 * 
 * Stored keys:
 * - "refresh:hash:abc123..." → "42"
 * - "refresh:user:42" → "abc123..."
 * 
 * WHY HASH THE TOKEN?
 * -------------------
 * If Redis is compromised, attackers get only hashes, not real tokens.
 * They can't use hashes to impersonate users.
 * This is similar to how we hash passwords in the database.
 * 
 * TTL (Time-To-Live):
 * -------------------
 * Both keys expire after 30 days (TOKEN_TTL).
 * Redis automatically deletes expired keys - no cleanup code needed!
 * 
 * @see AuthService Where refresh tokens are created during login
 */
@Service  // Marks this as a Spring-managed service bean
public class RefreshTokenService {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Logger for debugging and security auditing */
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    /** Prefix for token → userId lookup. Example: "refresh:hash:abc123" */
    private static final String TOKEN_KEY_PREFIX = "refresh:hash:";
    
    /** Prefix for userId → token lookup. Example: "refresh:user:42" */
    private static final String USER_KEY_PREFIX = "refresh:user:";
    
    /** How long refresh tokens live in Redis (30 days) */
    private static final Duration TOKEN_TTL = Duration.ofDays(30);

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    /**
     * StringRedisTemplate - Spring's helper for Redis String operations.
     * 
     * WHY StringRedisTemplate?
     * - We only store strings (userId, hashed tokens)
     * - Simpler than generic RedisTemplate
     * - Handles serialization automatically
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor injection - Spring provides the Redis template.
     * The template is configured in RedisConfig class.
     */
    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========================================================================
    // CREATE TOKEN
    // ========================================================================

    /**
     * Stores a refresh token in Redis (hashed for security).
     * 
     * IMPORTANT: This method expects the RAW JWT token, not a hash!
     * It will hash the token internally before storing.
     * 
     * STEPS:
     * 1. Delete any existing token for this user (single-session)
     * 2. Hash the token with SHA-256
     * 3. Store token→userId mapping
     * 4. Store userId→token mapping
     * 5. Set TTL on both keys
     * 
     * @param token The raw JWT refresh token (not hashed)
     * @param userId The user's MySQL ID
     */
    public void createRefreshToken(String token, Long userId) {
        String userIdStr = userId.toString();
        
        // Delete any existing token - one token per user (single-session)
        deleteByUserId(userId);
        
        // Hash the token with SHA-256 for secure storage
        String hashedToken = hashToken(token);
        
        // Build Redis keys
        String tokenKey = TOKEN_KEY_PREFIX + hashedToken;  // "refresh:hash:abc123"
        String userKey = USER_KEY_PREFIX + userIdStr;      // "refresh:user:42"
        
        // Store both mappings with TTL (30 days)
        // opsForValue() = simple key-value operations
        redisTemplate.opsForValue().set(tokenKey, userIdStr, TOKEN_TTL);
        redisTemplate.opsForValue().set(userKey, hashedToken, TOKEN_TTL);
        
        logger.info("Stored refresh token for userId: {} with TTL: {} days", userId, TOKEN_TTL.toDays());
    }

    // ========================================================================
    // VALIDATE / FIND TOKEN
    // ========================================================================

    /**
     * Finds the userId associated with a refresh token.
     * Used to validate token and identify user during refresh.
     * 
     * STEPS:
     * 1. Hash the incoming token
     * 2. Look up "refresh:hash:{hash}" in Redis
     * 3. Return userId if found
     * 
     * @param token The raw JWT refresh token (will be hashed internally)
     * @return Optional containing userId if valid, empty if invalid/expired
     */
    public Optional<Long> findUserIdByToken(String token) {
        String hashedToken = hashToken(token);
        String tokenKey = TOKEN_KEY_PREFIX + hashedToken;
        
        // Query Redis for this token
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        
        if (userIdStr != null) {
            return Optional.of(Long.parseLong(userIdStr));
        }
        return Optional.empty();  // Token not found or expired
    }

    /**
     * Checks if a refresh token exists and is valid.
     * 
     * @param token The raw JWT refresh token
     * @return true if token exists in Redis, false otherwise
     */
    public boolean isValidToken(String token) {
        String hashedToken = hashToken(token);
        String tokenKey = TOKEN_KEY_PREFIX + hashedToken;
        
        // hasKey returns Boolean, could be null, so we explicitly check for TRUE
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }

    // ========================================================================
    // DELETE TOKEN
    // ========================================================================

    /**
     * Deletes a refresh token by the raw token string.
     * Used during logout to invalidate the token.
     * 
     * STEPS:
     * 1. Hash the token
     * 2. Look up userId from token key
     * 3. Delete user→token mapping
     * 4. Delete token→userId mapping
     * 
     * @param token The raw JWT refresh token
     */
    public void deleteByToken(String token) {
        String hashedToken = hashToken(token);
        String tokenKey = TOKEN_KEY_PREFIX + hashedToken;
        
        // First get userId so we can delete the user→token mapping too
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        
        if (userIdStr != null) {
            String userKey = USER_KEY_PREFIX + userIdStr;
            redisTemplate.delete(userKey);  // Delete user→token mapping
        }
        redisTemplate.delete(tokenKey);  // Delete token→userId mapping
        
        logger.debug("Deleted refresh token");
    }

    /**
     * Deletes all refresh tokens for a user.
     * Used when:
     * - User logs in again (single-session: old token invalid)
     * - Admin revokes user's sessions
     * 
     * STEPS:
     * 1. Look up hashed token from user key
     * 2. Delete token→userId mapping
     * 3. Delete user→token mapping
     * 
     * @param userId The user's MySQL ID
     */
    public void deleteByUserId(Long userId) {
        String userIdStr = userId.toString();
        String userKey = USER_KEY_PREFIX + userIdStr;
        
        // Get the hashed token so we can delete the token key too
        String hashedToken = redisTemplate.opsForValue().get(userKey);
        
        if (hashedToken != null) {
            String tokenKey = TOKEN_KEY_PREFIX + hashedToken;
            redisTemplate.delete(tokenKey);  // Delete token→userId mapping
        }
        redisTemplate.delete(userKey);  // Delete user→token mapping
        
        logger.debug("Deleted refresh token for userId: {}", userId);
    }

    // ========================================================================
    // HASHING UTILITY
    // ========================================================================

    /**
     * Hashes a token using SHA-256 for secure storage.
     * 
     * WHY SHA-256?
     * - One-way hash: can't reverse to get original token
     * - Fast: good for validating on every request
     * - Standard: widely used, well-tested
     * 
     * WHY BASE64 ENCODING?
     * - SHA-256 outputs bytes, but Redis keys are strings
     * - Base64 URL-safe encoding converts bytes to ASCII string
     * - "withoutPadding" removes trailing '=' for cleaner keys
     * 
     * EXAMPLE:
     * Input:  "eyJhbGciOiJIUzUxMi..."
     * Output: "K7gNU3sdo-OL0wNhqoVWhr3g6s1xYv72ol_pe_Unols"
     * 
     * @param token The raw token to hash
     * @return Base64-encoded SHA-256 hash
     */
    private String hashToken(String token) {
        try {
            // Get SHA-256 digest instance
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Hash the token (converted to UTF-8 bytes)
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            // Convert to URL-safe Base64 string (no padding)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 should always be available in Java
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}