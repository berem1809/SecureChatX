package com.chatapp.util;

// ============================================================================
// IMPORTS
// ============================================================================

// Spring Security's BCrypt password encoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * ============================================================================
 * PASSWORD UTILITY - Helper class for password hashing operations
 * ============================================================================
 * 
 * WHAT IS PASSWORD HASHING?
 * -------------------------
 * Hashing converts a password into an irreversible string of characters.
 * Even if attackers get the hash, they can't reverse it to get the password.
 * 
 * PASSWORD STORAGE RULE #1:
 * NEVER STORE PLAIN TEXT PASSWORDS!
 * 
 * If you store plain text:
 * - Database breach = all passwords exposed
 * - Users often reuse passwords
 * - One breach affects their other accounts
 * 
 * WHY BCRYPT?
 * -----------
 * BCrypt is specifically designed for password hashing:
 * 
 * 1. SLOW BY DESIGN
 *    - Takes ~100ms to hash
 *    - Attackers can only try ~10 passwords/second
 *    - Compare to MD5: millions/second
 * 
 * 2. BUILT-IN SALT
 *    - Each hash includes random salt
 *    - Same password → different hash each time
 *    - Rainbow table attacks don't work
 * 
 * 3. ADAPTIVE COST FACTOR
 *    - Configurable work factor (default 10)
 *    - Cost 10 = 2^10 = 1024 iterations
 *    - Can increase as hardware gets faster
 * 
 * BCRYPT FORMAT:
 * --------------
 * $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 * |  |  |                                                    |
 * |  |  |                                                    └─ Hash (31 chars)
 * |  |  └─ Salt (22 chars)
 * |  └─ Cost factor (10 = 2^10 iterations)
 * └─ BCrypt identifier (2a)
 * 
 * EXAMPLE:
 * --------
 * Password: "MyPassword123"
 * 
 * First hash:  $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 * Second hash: $2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDMVr5yUP1KUOYTa
 * 
 * Same password, different hashes (because of different salts)!
 * But BCrypt.matches() still works because salt is stored in the hash.
 * 
 * WHEN TO USE THIS CLASS:
 * -----------------------
 * NOTE: In our app, we actually use Spring's PasswordEncoder bean
 * (configured in SecurityConfig) injected via dependency injection.
 * 
 * This utility class is useful for:
 * - Standalone scripts
 * - Testing
 * - Situations where DI isn't available
 * 
 * For the main app, prefer: @Autowired PasswordEncoder passwordEncoder;
 * 
 * @see SecurityConfig Where the PasswordEncoder bean is created
 * @see AuthService Where password encoding happens during registration
 */
public class PasswordUtil {

    // ========================================================================
    // STATIC ENCODER INSTANCE
    // ========================================================================
    
    /**
     * Shared BCryptPasswordEncoder instance.
     * 
     * WHY STATIC?
     * - BCryptPasswordEncoder is stateless and thread-safe
     * - No need to create new instance for each operation
     * - Default cost factor is 10 (2^10 = 1024 iterations)
     * 
     * CUSTOMIZING COST:
     * new BCryptPasswordEncoder(12)  // 2^12 = 4096 iterations (slower, more secure)
     * new BCryptPasswordEncoder(8)   // 2^8 = 256 iterations (faster, less secure)
     */
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ========================================================================
    // ENCODING (Hashing)
    // ========================================================================

    /**
     * Hashes a plain text password using BCrypt.
     * 
     * USE THIS WHEN:
     * - User registers (hash password before storing)
     * - User changes password (hash new password)
     * - Admin resets user password
     * 
     * NEVER:
     * - Store the plain text password
     * - Log the plain text password
     * - Send the plain text password in response
     * 
     * @param rawPassword The plain text password from user input
     * @return BCrypt hash string (60 characters)
     * 
     * EXAMPLE:
     * String hash = PasswordUtil.encode("MyPassword123");
     * // hash = "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
     * // Store this hash in database!
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // ========================================================================
    // VERIFICATION (Matching)
    // ========================================================================

    /**
     * Verifies if a raw password matches an encoded password hash.
     * 
     * USE THIS WHEN:
     * - User logs in (verify entered password against stored hash)
     * - User changes password (verify old password first)
     * 
     * HOW IT WORKS:
     * 1. Extract salt from the encoded password
     * 2. Hash the raw password with that salt
     * 3. Compare the two hashes
     * 
     * @param rawPassword The plain text password from user input
     * @param encodedPassword The BCrypt hash from database
     * @return true if passwords match, false otherwise
     * 
     * EXAMPLE:
     * String storedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye...";
     * boolean valid = PasswordUtil.matches("MyPassword123", storedHash);
     * // valid = true (if password is correct)
     * 
     * SECURITY NOTE:
     * This method uses constant-time comparison to prevent timing attacks.
     * Even if password is wrong, execution time is the same.
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
    
    /*
     * COMMON MISTAKES TO AVOID:
     * =========================
     * 
     * 1. COMPARING HASHES DIRECTLY:
     *    ❌ storedHash.equals(encode(inputPassword))  // WRONG!
     *    ✅ matches(inputPassword, storedHash)         // CORRECT!
     *    
     *    Why? Each encode() produces different hash (different salt).
     * 
     * 2. USING WEAK ALGORITHMS:
     *    ❌ MD5, SHA-1, SHA-256 (too fast, no salt)
     *    ✅ BCrypt, Argon2, PBKDF2 (designed for passwords)
     * 
     * 3. STORING ENCRYPTED (not hashed) PASSWORDS:
     *    ❌ AES.encrypt(password, key)  // Can be decrypted!
     *    ✅ BCrypt.encode(password)      // Cannot be reversed!
     * 
     * 4. LOGGING PASSWORDS:
     *    ❌ logger.info("User {} login with password {}", email, password);
     *    ✅ logger.info("User {} login attempt", email);
     */
}