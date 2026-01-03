package com.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for managing refresh tokens in Redis.
 * 
 * Token storage strategy (hashed for security):
 * - Key: "refresh:hash:{hashedToken}" -> Value: userId (as string)
 * - Key: "refresh:user:{userId}" -> Value: hashedToken
 * 
 * TTL is set automatically (30 days), Redis handles expiration.
 * Tokens are hashed using SHA-256 before storage for security.
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final String TOKEN_KEY_PREFIX = "refresh:hash:";
    private static final String USER_KEY_PREFIX = "refresh:user:";
    private static final Duration TOKEN_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Stores a refresh token in Redis (hashed) with TTL.
     * Deletes any existing token for the user first.
     * 
     * @param token The raw JWT refresh token
     * @param userId The user ID (Long from MySQL)
     */
    public void createRefreshToken(String token, Long userId) {
        String userIdStr = userId.toString();
        // Delete any existing token for this user
        deleteByUserId(userId);
        
        String hashedToken = hashToken(token);
        String tokenKey = TOKEN_KEY_PREFIX + hashedToken;
        String userKey = USER_KEY_PREFIX + userIdStr;
        
        // Store both mappings with TTL
        redisTemplate.opsForValue().set(tokenKey, userIdStr, TOKEN_TTL);
        redisTemplate.opsForValue().set(userKey, hashedToken, TOKEN_TTL);
        
        logger.info("Stored refresh token for userId: {} with TTL: {} days", userId, TOKEN_TTL.toDays());
    }

    /**
     * Validates a refresh token and returns the associated userId.
     * 
     * @param token The raw JWT refresh token
     * @return Optional containing userId if token is valid
     */
    public Optional<Long> findUserIdByToken(String token) {
        String hashedToken = hashToken(token);
        String tokenKey = TOKEN_KEY_PREFIX + hashedToken;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        if (userIdStr != null) {
            return Optional.of(Long.parseLong(userIdStr));
        }
        return Optional.empty();
    }

    /**
     * Checks if a refresh token exists and is valid.
     * 
     * @param token The raw JWT refresh token
     * @return true if token exists in Redis
     */
    public boolean isValidToken(String token) {
        String hashedToken = hashToken(token);
        String tokenKey = TOKEN_KEY_PREFIX + hashedToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }

    /**
     * Deletes a refresh token by the raw token string.
     * 
     * @param token The raw JWT refresh token
     */
    public void deleteByToken(String token) {
        String hashedToken = hashToken(token);
        String tokenKey = TOKEN_KEY_PREFIX + hashedToken;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        
        if (userIdStr != null) {
            String userKey = USER_KEY_PREFIX + userIdStr;
            redisTemplate.delete(userKey);
        }
        redisTemplate.delete(tokenKey);
        logger.debug("Deleted refresh token");
    }

    /**
     * Deletes all refresh tokens for a user.
     * 
     * @param userId The user ID (Long from MySQL)
     */
    public void deleteByUserId(Long userId) {
        String userIdStr = userId.toString();
        String userKey = USER_KEY_PREFIX + userIdStr;
        String hashedToken = redisTemplate.opsForValue().get(userKey);
        
        if (hashedToken != null) {
            String tokenKey = TOKEN_KEY_PREFIX + hashedToken;
            redisTemplate.delete(tokenKey);
        }
        redisTemplate.delete(userKey);
        logger.debug("Deleted refresh token for userId: {}", userId);
    }

    /**
     * Hashes a token using SHA-256 for secure storage.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}