package com.chatapp.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * ============================================================================
 * ENCRYPTION UTILITY - Cryptographic key generation
 * ============================================================================
 * 
 * Provides secure random key generation for group chat encryption.
 * Uses URL-safe Base64 encoding for better JavaScript compatibility.
 * 
 * KEY FEATURES:
 * - SecureRandom for cryptographically strong random keys
 * - URL-safe Base64 encoding (no padding) for web compatibility
 * - Consistent 32-byte (256-bit) keys for XSalsa20-Poly1305
 */
public class EncryptionUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a URL-safe base64-encoded secure key.
     * URL-safe encoding is compatible with JavaScript libraries and avoids
     * issues with '+' and '/' characters in standard Base64.
     *
     * @param numBytes The number of bytes for the key (e.g., 32 for a 256-bit key)
     * @return A URL-safe base64-encoded string representation of the key
     */
    public static String generateSecureKey(int numBytes) {
        byte[] key = new byte[numBytes];
        SECURE_RANDOM.nextBytes(key);
        
        // Use URL-safe encoding without padding for better JavaScript compatibility
        // This replaces '+' with '-' and '/' with '_', and removes '=' padding
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key);
    }

    /**
     * Decode a URL-safe base64 key back to bytes.
     * Useful for key validation and testing.
     *
     * @param encodedKey The URL-safe base64-encoded key
     * @return The decoded key bytes
     */
    public static byte[] decodeKey(String encodedKey) {
        return Base64.getUrlDecoder().decode(encodedKey);
    }

    /**
     * Validate that a key is the correct length when decoded.
     *
     * @param encodedKey The base64-encoded key to validate
     * @param expectedBytes The expected number of bytes (e.g., 32)
     * @return true if the key is valid, false otherwise
     */
    public static boolean isValidKey(String encodedKey, int expectedBytes) {
        try {
            byte[] decoded = decodeKey(encodedKey);
            return decoded.length == expectedBytes;
        } catch (Exception e) {
            return false;
        }
    }
}