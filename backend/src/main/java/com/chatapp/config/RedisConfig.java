package com.chatapp.config;

// ============================================================================
// IMPORTS
// ============================================================================

// Spring Framework annotations
import org.springframework.context.annotation.Bean;          // Marks methods that create Spring beans
import org.springframework.context.annotation.Configuration; // Marks this as a configuration class

// Spring Data Redis imports
import org.springframework.data.redis.connection.RedisConnectionFactory; // Creates connections to Redis
import org.springframework.data.redis.core.RedisTemplate;                // Main API for Redis operations
import org.springframework.data.redis.core.StringRedisTemplate;          // Specialized template for Strings
import org.springframework.data.redis.serializer.StringRedisSerializer;  // Converts Java objects to Redis strings

/**
 * ============================================================================
 * REDIS CONFIGURATION - Setup for Redis in-memory data store
 * ============================================================================
 * 
 * WHAT IS REDIS?
 * --------------
 * Redis (REmote DIctionary Server) is an in-memory data store that's:
 * - FAST: Data stored in RAM, not disk (sub-millisecond operations)
 * - SIMPLE: Key-value storage like a HashMap
 * - PERSISTENT: Can save to disk if needed
 * - SUPPORTS TTL: Data can auto-expire after a set time
 * 
 * WHY USE REDIS IN THIS APP?
 * --------------------------
 * We use Redis for TEMPORARY authentication data:
 * 
 * 1. VERIFICATION TOKENS (TTL: 24 hours)
 *    - Created when user registers
 *    - Deleted when user clicks verification link
 *    - Auto-expires after 24 hours if not used
 *    
 * 2. REFRESH TOKENS (TTL: 30 days)
 *    - Created when user logs in
 *    - Deleted when user logs out
 *    - Auto-expires after 30 days if not used
 * 
 * WHY NOT MYSQL FOR TOKENS?
 * -------------------------
 * | Feature          | MySQL                    | Redis                     |
 * |------------------|--------------------------|---------------------------|
 * | Speed            | Slower (disk-based)      | Fast (RAM-based)          |
 * | TTL Support      | Manual (need cron jobs)  | Built-in (automatic)      |
 * | Persistence      | Always                   | Optional                  |
 * | Best for         | Permanent data (users)   | Temporary data (tokens)   |
 * 
 * REDIS DATA STRUCTURE IN THIS APP:
 * ----------------------------------
 * Verification Tokens:
 *   Key: "verification:token:{uuid}"     → Value: "userId"
 *   Key: "verification:user:{userId}"    → Value: "uuid"
 *   TTL: 24 hours
 * 
 * Refresh Tokens:
 *   Key: "refresh:hash:{hashedToken}"    → Value: "userId"
 *   Key: "refresh:user:{userId}"         → Value: "hashedToken"
 *   TTL: 30 days
 * 
 * EXAMPLE:
 * --------
 * User registers → Redis stores:
 *   "verification:token:550e8400-e29b-41d4-a716-446655440000" → "42"
 *   "verification:user:42" → "550e8400-e29b-41d4-a716-446655440000"
 *   (Both expire in 24 hours)
 * 
 * HOW TO VIEW REDIS DATA:
 * -----------------------
 * Open terminal and run:
 *   redis-cli           # Connect to Redis
 *   KEYS *              # Show all keys
 *   GET "key-name"      # Get value of a key
 *   TTL "key-name"      # See remaining time to live (seconds)
 * 
 * @see VerificationTokenService Uses Redis for verification tokens
 * @see RefreshTokenService Uses Redis for refresh tokens
 */
@Configuration  // Marks this as a Spring configuration class
public class RedisConfig {

    // ========================================================================
    // REDIS TEMPLATE BEAN - Main API for Redis operations
    // ========================================================================
    
    /**
     * Creates a RedisTemplate configured for String key-value operations.
     * 
     * WHAT IS RedisTemplate?
     * ----------------------
     * RedisTemplate is Spring's main interface for working with Redis.
     * It's like JdbcTemplate for databases, but for Redis.
     * 
     * Provides methods like:
     * - opsForValue().set(key, value)        → Store a value
     * - opsForValue().get(key)               → Retrieve a value
     * - opsForValue().set(key, value, TTL)   → Store with expiration
     * - delete(key)                          → Delete a key
     * - hasKey(key)                          → Check if key exists
     * 
     * WHAT IS SERIALIZATION?
     * ----------------------
     * Serialization converts Java objects to a format that can be stored in Redis.
     * We use StringRedisSerializer to store everything as readable strings.
     * 
     * Without proper serialization, your keys might look like:
     *   "\xac\xed\x00\x05t\x00\x08myKey" (unreadable binary)
     * 
     * With StringRedisSerializer:
     *   "myKey" (clean, readable string)
     * 
     * @param connectionFactory Factory that creates Redis connections
     *                          (auto-configured by Spring from application.properties)
     * @return Configured RedisTemplate for String operations
     */
    @Bean  // Spring will manage this object and inject it where needed
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        
        // Create a new RedisTemplate instance
        // <String, String> means both keys and values are Strings
        RedisTemplate<String, String> template = new RedisTemplate<>();
        
        // Set the connection factory (how to connect to Redis server)
        // Spring auto-creates this from application.properties:
        //   spring.redis.host=localhost
        //   spring.redis.port=6379
        template.setConnectionFactory(connectionFactory);
        
        // ====================================================================
        // CONFIGURE SERIALIZERS - How Java objects are converted to Redis format
        // ====================================================================
        
        // Serializer for regular keys (e.g., "verification:token:abc123")
        template.setKeySerializer(new StringRedisSerializer());
        
        // Serializer for regular values (e.g., "42" for userId)
        template.setValueSerializer(new StringRedisSerializer());
        
        // Serializer for hash keys (when using Redis hashes)
        // Hash example: HSET user:42 name "John" email "john@example.com"
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Serializer for hash values
        template.setHashValueSerializer(new StringRedisSerializer());
        
        // Initialize the template (validates configuration)
        // Must be called after all properties are set
        template.afterPropertiesSet();
        
        return template;
    }

    // ========================================================================
    // STRING REDIS TEMPLATE BEAN - Convenience template for Strings
    // ========================================================================
    
    /**
     * Creates a StringRedisTemplate - a simpler template for String operations.
     * 
     * WHAT'S THE DIFFERENCE FROM RedisTemplate<String, String>?
     * ---------------------------------------------------------
     * StringRedisTemplate is essentially the same as RedisTemplate<String, String>
     * but with these benefits:
     * 
     * 1. PRE-CONFIGURED: Already uses StringRedisSerializer for everything
     * 2. SIMPLER: Less code to write
     * 3. TYPE-SAFE: Specifically for String operations
     * 
     * WHEN TO USE WHICH?
     * ------------------
     * - StringRedisTemplate: Most cases (we store strings)
     * - RedisTemplate<K, V>: When storing complex objects or need custom serialization
     * 
     * USAGE EXAMPLE:
     * --------------
     * @Autowired
     * StringRedisTemplate redisTemplate;
     * 
     * // Store a value with TTL
     * redisTemplate.opsForValue().set("myKey", "myValue", Duration.ofHours(24));
     * 
     * // Retrieve a value
     * String value = redisTemplate.opsForValue().get("myKey");
     * 
     * // Delete a key
     * redisTemplate.delete("myKey");
     * 
     * @param connectionFactory Factory that creates Redis connections
     * @return StringRedisTemplate instance
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        // StringRedisTemplate auto-configures with StringRedisSerializer
        // Just need to pass the connection factory
        return new StringRedisTemplate(connectionFactory);
    }
}
