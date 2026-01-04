package com.chatapp.config;

// ============================================================================
// IMPORTS
// ============================================================================

// Bucket4j library - Industry standard for rate limiting in Java
// Website: https://github.com/bucket4j/bucket4j
import io.github.bucket4j.Bandwidth;  // Defines the rate limit rules (how many tokens, how fast they refill)
import io.github.bucket4j.Bucket;     // The "bucket" that holds tokens - each request consumes a token
import io.github.bucket4j.Refill;     // Defines how tokens are added back to the bucket

// Spring Framework annotations
import org.springframework.context.annotation.Configuration;  // Marks this as a config class
import org.springframework.stereotype.Component;              // Makes this class a Spring bean

// Java time and collections
import java.time.Duration;                    // For specifying time periods (e.g., 1 minute)
import java.util.Map;                         // Interface for key-value storage
import java.util.concurrent.ConcurrentHashMap; // Thread-safe Map implementation

/**
 * ============================================================================
 * RATE LIMIT CONFIGURATION - Protects against brute-force and abuse attacks
 * ============================================================================
 * 
 * WHAT IS RATE LIMITING?
 * ----------------------
 * Rate limiting restricts how many requests a client (identified by IP address)
 * can make to our API within a time window. This protects against:
 * 
 * 1. BRUTE-FORCE ATTACKS: Hackers trying thousands of passwords
 * 2. DENIAL OF SERVICE (DoS): Overwhelming the server with requests
 * 3. SPAM: Bots creating fake accounts
 * 4. RESOURCE ABUSE: One user consuming all server resources
 * 
 * HOW THE TOKEN BUCKET ALGORITHM WORKS:
 * -------------------------------------
 * Imagine each user has a "bucket" that holds tokens (like coins):
 * 
 *   [Token] [Token] [Token] [Token] [Token]  ← Bucket with 5 tokens
 *   
 * 1. Each API request CONSUMES one token from the bucket
 * 2. If bucket is EMPTY, request is REJECTED (429 Too Many Requests)
 * 3. Tokens are automatically REFILLED over time
 * 
 * EXAMPLE - Login Rate Limit (5 per minute):
 * ------------------------------------------
 * Time 0:00 - Bucket starts full: [●][●][●][●][●] (5 tokens)
 * Time 0:01 - Login attempt 1:    [●][●][●][●][ ] (4 tokens) ✓ Allowed
 * Time 0:02 - Login attempt 2:    [●][●][●][ ][ ] (3 tokens) ✓ Allowed
 * Time 0:03 - Login attempt 3:    [●][●][ ][ ][ ] (2 tokens) ✓ Allowed
 * Time 0:04 - Login attempt 4:    [●][ ][ ][ ][ ] (1 token)  ✓ Allowed
 * Time 0:05 - Login attempt 5:    [ ][ ][ ][ ][ ] (0 tokens) ✓ Allowed
 * Time 0:06 - Login attempt 6:    [ ][ ][ ][ ][ ] (0 tokens) ✗ REJECTED! 429 Error
 * Time 1:00 - Bucket refills:     [●][●][●][●][●] (5 tokens) - Can try again!
 * 
 * RATE LIMITS IN THIS APPLICATION:
 * --------------------------------
 * | Endpoint           | Limit        | Why?                              |
 * |--------------------|--------------|-----------------------------------|
 * | /api/auth/login    | 5/minute     | Prevent password guessing         |
 * | /api/auth/register | 3/minute     | Prevent spam account creation     |
 * | All other APIs     | 100/minute   | General abuse protection          |
 * 
 * WHY DIFFERENT LIMITS?
 * - Login: Strict limit because wrong passwords could mean attack
 * - Register: Very strict because creating accounts is sensitive
 * - General: More relaxed for normal API usage
 * 
 * @see RateLimitFilter Where these buckets are actually used
 */
@Configuration  // Tells Spring this class contains configuration settings
@Component      // Registers this class as a Spring bean (singleton by default)
public class RateLimitConfig {

    // ========================================================================
    // BUCKET STORAGE - One bucket per IP address per endpoint type
    // ========================================================================
    
    /**
     * Stores rate limit buckets for LOGIN attempts, keyed by IP address.
     * 
     * Example contents:
     * {
     *   "192.168.1.1": Bucket(5 tokens),
     *   "10.0.0.50": Bucket(3 tokens),
     *   "203.45.67.89": Bucket(0 tokens)  // This IP is rate limited!
     * }
     * 
     * WHY ConcurrentHashMap?
     * - Multiple requests can arrive simultaneously from different IPs
     * - ConcurrentHashMap is THREAD-SAFE (handles concurrent access)
     * - Regular HashMap would cause race conditions and data corruption
     */
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    
    /**
     * Stores rate limit buckets for REGISTRATION attempts.
     * Separate from login because registration has stricter limits (3/min vs 5/min).
     */
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    
    /**
     * Stores rate limit buckets for all OTHER API endpoints.
     * More generous limit (100/min) for normal API operations.
     */
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    // ========================================================================
    // PUBLIC METHODS - Called by RateLimitFilter to get/create buckets
    // ========================================================================
    
    /**
     * Gets (or creates) a rate limit bucket for LOGIN attempts from a specific IP.
     * 
     * HOW computeIfAbsent() WORKS:
     * 1. Check if bucket exists for this IP address
     * 2. If YES: Return the existing bucket
     * 3. If NO: Create new bucket using createLoginBucket(), store it, return it
     * 
     * This is ATOMIC (thread-safe) - prevents duplicate buckets for same IP.
     * 
     * EXAMPLE:
     * First request from 192.168.1.1:
     *   - No bucket exists → Creates new bucket with 5 tokens → Returns it
     * Second request from 192.168.1.1:
     *   - Bucket exists (maybe 4 tokens left) → Returns existing bucket
     * 
     * @param ipAddress The client's IP address (e.g., "192.168.1.1")
     * @return Bucket for this IP's login attempts
     */
    public Bucket getLoginBucket(String ipAddress) {
        // computeIfAbsent: "compute if absent" = create only if doesn't exist
        // 'k' is the key (ipAddress) - we ignore it since all login buckets are identical
        return loginBuckets.computeIfAbsent(ipAddress, k -> createLoginBucket());
    }

    /**
     * Gets (or creates) a rate limit bucket for REGISTRATION attempts from a specific IP.
     * 
     * Registration has STRICTER limits (3/minute) because:
     * - Creating accounts is more sensitive than logging in
     * - Spam bots try to create many fake accounts
     * - Each registration sends an email (costs money, could be abused)
     * 
     * @param ipAddress The client's IP address
     * @return Bucket for this IP's registration attempts
     */
    public Bucket getRegisterBucket(String ipAddress) {
        return registerBuckets.computeIfAbsent(ipAddress, k -> createRegisterBucket());
    }

    /**
     * Gets (or creates) a rate limit bucket for GENERAL API requests from a specific IP.
     * 
     * General bucket has MORE GENEROUS limits (100/minute) because:
     * - Normal users make many API calls (loading data, etc.)
     * - We don't want to block legitimate usage
     * - Still prevents abuse (100/min is reasonable)
     * 
     * @param ipAddress The client's IP address
     * @return Bucket for this IP's general API requests
     */
    public Bucket getGeneralBucket(String ipAddress) {
        return generalBuckets.computeIfAbsent(ipAddress, k -> createGeneralBucket());
    }

    // ========================================================================
    // PRIVATE METHODS - Create buckets with specific rate limits
    // ========================================================================
    
    /**
     * Creates a new bucket for LOGIN rate limiting.
     * 
     * CONFIGURATION:
     * - Capacity: 5 tokens (maximum requests allowed)
     * - Refill: 5 tokens every 1 minute (greedy = all at once)
     * 
     * BANDWIDTH EXPLAINED:
     * Bandwidth.classic(capacity, refill) creates a "classic" token bucket:
     * - capacity: Maximum tokens the bucket can hold
     * - refill: How tokens are added back over time
     * 
     * REFILL STRATEGIES:
     * - Refill.greedy(5, Duration.ofMinutes(1)):
     *   Adds ALL 5 tokens at the START of each minute (burst-friendly)
     *   Good for: Users who make several quick requests then wait
     * 
     * - Refill.intervally(5, Duration.ofMinutes(1)):
     *   Adds 5 tokens spread evenly throughout the minute (1 every 12 sec)
     *   Good for: Steady rate limiting without bursts
     * 
     * We use GREEDY because login attempts often come in bursts
     * (user typing wrong password a few times).
     * 
     * @return New bucket configured for login rate limiting
     */
    private Bucket createLoginBucket() {
        // Create bandwidth rule: 5 tokens max, refill 5 tokens every minute
        Bandwidth limit = Bandwidth.classic(
            5,                                    // Bucket capacity (max tokens)
            Refill.greedy(5, Duration.ofMinutes(1)) // Refill 5 tokens per minute
        );
        
        // Build and return the bucket with this limit
        return Bucket.builder()
            .addLimit(limit)  // Add our bandwidth rule
            .build();         // Create the bucket
    }

    /**
     * Creates a new bucket for REGISTRATION rate limiting.
     * 
     * STRICTER THAN LOGIN: Only 3 attempts per minute because:
     * - Registration creates database records
     * - Registration sends emails (costs resources)
     * - Spam bots often target registration endpoints
     * 
     * @return New bucket configured for registration rate limiting
     */
    private Bucket createRegisterBucket() {
        // Stricter limit: only 3 registrations per minute
        Bandwidth limit = Bandwidth.classic(
            3,                                    // Only 3 tokens
            Refill.greedy(3, Duration.ofMinutes(1)) // Refill 3 per minute
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Creates a new bucket for GENERAL API rate limiting.
     * 
     * MORE GENEROUS: 100 requests per minute because:
     * - Normal API usage involves many requests (fetching messages, etc.)
     * - We don't want to impact legitimate users
     * - 100/min still prevents serious abuse
     * 
     * @return New bucket configured for general API rate limiting
     */
    private Bucket createGeneralBucket() {
        // Generous limit: 100 requests per minute
        Bandwidth limit = Bandwidth.classic(
            100,                                    // 100 tokens
            Refill.greedy(100, Duration.ofMinutes(1)) // Refill 100 per minute
        );
        return Bucket.builder().addLimit(limit).build();
    }

    // ========================================================================
    // MAINTENANCE METHODS
    // ========================================================================
    
    /**
     * Clears all stored buckets to free memory.
     * 
     * WHY CLEAR BUCKETS?
     * - Over time, buckets accumulate for every unique IP that hits the API
     * - Old IPs that haven't made requests in a while waste memory
     * - This method can be called periodically (e.g., every hour) by a scheduled task
     * 
     * WHAT HAPPENS AFTER CLEARING?
     * - All IPs get fresh buckets on their next request
     * - Previously rate-limited IPs can make requests again
     * - This is usually fine since most rate limits reset within a minute anyway
     * 
     * FUTURE IMPROVEMENT:
     * Instead of clearing ALL buckets, we could:
     * - Remove only buckets that haven't been used in X minutes
     * - Use a cache library with automatic eviction (like Caffeine)
     */
    public void clearBuckets() {
        loginBuckets.clear();     // Remove all login buckets
        registerBuckets.clear();  // Remove all registration buckets
        generalBuckets.clear();   // Remove all general buckets
    }
}
