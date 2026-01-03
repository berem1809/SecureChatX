package com.chatapp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class for password operations.
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Encodes a plain text password using BCrypt.
     * @param rawPassword the plain text password
     * @return the encoded password
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * Verifies if a raw password matches the encoded password.
     * @param rawPassword the plain text password
     * @param encodedPassword the encoded password
     * @return true if matches, false otherwise
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}