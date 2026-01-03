package com.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing email verification tokens in Redis.
 * 
 * Token storage strategy:
 * - Key: "verification:token:{token}" -> Value: userId (as string)
 * - Key: "verification:user:{userId}" -> Value: token
 * 
 * TTL is set automatically (24 hours), so Redis handles expiration.
 */
@Service
public class VerificationTokenService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenService.class);
    
    private static final String TOKEN_KEY_PREFIX = "verification:token:";
    private static final String USER_KEY_PREFIX = "verification:user:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public VerificationTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Creates a verification token for a user and stores it in Redis with TTL.
     * Deletes any existing token for the user first.
     * 
     * @param userId The user ID (Long from MySQL)
     * @return The generated token string
     */
    public String createVerificationToken(Long userId) {
        String userIdStr = userId.toString();
        // Delete any existing token for this user
        deleteByUserId(userId);
        
        String token = UUID.randomUUID().toString();
        String tokenKey = TOKEN_KEY_PREFIX + token;
        String userKey = USER_KEY_PREFIX + userIdStr;
        
        // Store both mappings with TTL
        redisTemplate.opsForValue().set(tokenKey, userIdStr, TOKEN_TTL);
        redisTemplate.opsForValue().set(userKey, token, TOKEN_TTL);
        
        logger.info("Created verification token for userId: {} with TTL: {} hours", userId, TOKEN_TTL.toHours());
        return token;
    }

    /**
     * Finds the userId associated with a verification token.
     * 
     * @param token The verification token
     * @return Optional containing userId if token exists and is valid
     */
    public Optional<Long> findUserIdByToken(String token) {
        String tokenKey = TOKEN_KEY_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        if (userIdStr != null) {
            return Optional.of(Long.parseLong(userIdStr));
        }
        return Optional.empty();
    }

    /**
     * Deletes verification token by token string.
     * Also removes the reverse mapping (user -> token).
     * 
     * @param token The token to delete
     */
    public void deleteByToken(String token) {
        String tokenKey = TOKEN_KEY_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        
        if (userIdStr != null) {
            String userKey = USER_KEY_PREFIX + userIdStr;
            redisTemplate.delete(userKey);
        }
        redisTemplate.delete(tokenKey);
        logger.debug("Deleted verification token: {}", token);
    }

    /**
     * Deletes verification token by userId.
     * 
     * @param userId The user ID whose token should be deleted
     */
    public void deleteByUserId(Long userId) {
        String userIdStr = userId.toString();
        String userKey = USER_KEY_PREFIX + userIdStr;
        String token = redisTemplate.opsForValue().get(userKey);
        
        if (token != null) {
            String tokenKey = TOKEN_KEY_PREFIX + token;
            redisTemplate.delete(tokenKey);
        }
        redisTemplate.delete(userKey);
        logger.debug("Deleted verification token for userId: {}", userId);
    }

    /**
     * Checks if a verification token exists and is valid (not expired).
     * 
     * @param token The token to check
     * @return true if token exists
     */
    public boolean isValidToken(String token) {
        String tokenKey = TOKEN_KEY_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }
}
