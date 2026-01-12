package com.chatapp.security;

// ============================================================================
// IMPORTS
// ============================================================================

// JJWT library - Industry standard for JWT handling in Java
import io.jsonwebtoken.*;                    // Core JWT classes
import io.jsonwebtoken.security.Keys;        // For creating cryptographic keys

// Spring Framework imports
import org.springframework.beans.factory.annotation.Value;  // For reading config values
import org.springframework.stereotype.Component;            // Marks this as a Spring bean

// Repository import (not currently used but available for future enhancements)
import com.chatapp.repository.UserRepository;

// Jakarta annotation for post-construction initialization
import jakarta.annotation.PostConstruct;

// Java security and utility imports
import java.security.Key;    // Cryptographic key interface
import java.util.Date;       // For token timestamps
import java.util.Map;        // For storing custom claims

/**
 * ============================================================================
 * JWT TOKEN PROVIDER - Handles all JWT (JSON Web Token) operations
 * ============================================================================
 * 
 * This class is responsible for:
 * 1. Generating JWT access tokens (short-lived, for API calls)
 * 2. Generating JWT refresh tokens (long-lived, for getting new access tokens)
 * 3. Validating tokens (checking signature and expiry)
 * 4. Extracting information from tokens (like user email)
 * 
 * WHAT IS A JWT?
 * --------------
 * JWT (JSON Web Token) is a compact, URL-safe token format for securely
 * transmitting information between parties. It consists of three parts:
 * 
 * Header.Payload.Signature
 * 
 * Example: eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiw.abc123...
 * 
 * 1. HEADER (eyJhbGciOiJIUzUxMiJ9):
 *    - Contains token type (JWT) and signing algorithm (HS512)
 *    - Base64 encoded JSON: {"alg":"HS512","typ":"JWT"}
 * 
 * 2. PAYLOAD (eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiw):
 *    - Contains claims (data) like user email, expiry time, roles
 *    - Base64 encoded JSON: {"sub":"user@example.com","exp":1234567890}
 *    - IMPORTANT: Payload is NOT encrypted, just encoded! Don't put secrets here.
 * 
 * 3. SIGNATURE (abc123...):
 *    - Created by signing (Header + Payload) with our secret key
 *    - Ensures the token hasn't been tampered with
 *    - Only our server can create valid signatures (we have the secret)
 * 
 * WHY USE JWT FOR AUTHENTICATION?
 * -------------------------------
 * - STATELESS: Server doesn't need to store session data
 * - SCALABLE: Works across multiple servers (no session sharing needed)
 * - SELF-CONTAINED: Token includes all needed information
 * - STANDARD: Widely supported, many libraries available
 * 
 * SECURITY CONSIDERATIONS:
 * ------------------------
 * - Tokens can be decoded by anyone (use HTTPS!)
 * - Never put sensitive data in tokens (passwords, credit cards)
 * - Use short expiry times for access tokens
 * - Store refresh tokens securely (HttpOnly cookies)
 * - Use strong secret keys (at least 256 bits for HS512)
 * 
 * @see JwtAuthenticationFilter Where tokens are validated on each request
 * @see AuthService Where tokens are generated during login
 */
@Component  // Marks this class as a Spring-managed bean (singleton by default)
public class JwtTokenProvider {
    
    // ========================================================================
    // CONFIGURATION PROPERTIES
    // ========================================================================
    
    /**
     * The secret key used to sign and verify JWT tokens.
     * 
     * @Value annotation reads this from application.properties:
     * app.jwt.secret=c5066462ce39e0b1cf9c4eb79645f85d6502e06f4851d42490e670b1e6f37db9
     * 
     * SECURITY WARNING:
     * - This key should be at least 64 characters for HS512
     * - NEVER commit real secrets to version control
     * - In production, use environment variables or secret management
     * - If this key is compromised, ALL tokens become invalid/forgeable
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * How long access tokens are valid (in milliseconds).
     * 
     * From application.properties:
     * app.jwt.access-expiration-ms=900000 (15 minutes = 900,000 ms)
     * 
     * WHY 15 MINUTES?
     * - Short enough to limit damage if token is stolen
     * - Long enough to not annoy users with constant re-authentication
     * - Refresh tokens handle longer sessions
     */
    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    /**
     * How long refresh tokens are valid (in milliseconds).
     * 
     * From application.properties:
     * app.jwt.refresh-expiration-ms=2592000000 (30 days)
     * 
     * WHY 30 DAYS?
     * - Provides good user experience (stay logged in)
     * - Refresh tokens are stored more securely (HttpOnly cookies + Redis)
     * - Can be revoked server-side by deleting from Redis
     */
    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * The cryptographic key object used for signing.
     * Created from the jwtSecret string in init().
     */
    private Key key;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================
    
    /**
     * Initializes the signing key after all properties are injected.
     * 
     * @PostConstruct runs AFTER the constructor and AFTER @Value injection.
     * This is necessary because jwtSecret is not available in the constructor.
     * 
     * Keys.hmacShaKeyFor() creates a proper cryptographic key from our secret.
     * HMAC = Hash-based Message Authentication Code
     * SHA = Secure Hash Algorithm
     */
    @PostConstruct
    public void init() {
        // Convert string secret to bytes, then to a proper Key object
        key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // ========================================================================
    // TOKEN GENERATION METHODS
    // ========================================================================
    
    /**
     * Generates a JWT access token for API authentication.
     * 
     * Access tokens are short-lived (15 min) and contain:
     * - Subject: User's email address
     * - Custom claims: User roles, permissions, etc.
     * - Issue time: When the token was created
     * - Expiration: When the token becomes invalid
     * - Signature: Proves the token is authentic
     * 
     * EXAMPLE OUTPUT:
     * eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTY0MDAwMDAwMCwiZXhwIjoxNjQwMDAwOTAwfQ.abc123...
     * 
     * @param subject The user identifier (usually email)
     * @param claims Additional data to include in token (roles, etc.)
     * @return The generated JWT token string
     */
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Date now = new Date();  // Current timestamp
        Date exp = new Date(now.getTime() + accessExpirationMs);  // Expiry = now + 15 min
        
        return Jwts.builder()
            .setSubject(subject)         // Main identifier (user email)
            .addClaims(claims)           // Additional data (roles, etc.)
            .setIssuedAt(now)            // When token was created
            .setExpiration(exp)          // When token expires
            .signWith(key, SignatureAlgorithm.HS512)  // Sign with our secret using HS512
            .compact();                  // Build the final token string
    }

    /**
     * Generates a JWT refresh token for obtaining new access tokens.
     * 
     * Refresh tokens are:
     * - Long-lived (30 days)
     * - Stored securely (HttpOnly cookie + Redis hash)
     * - Only contain the subject (minimal data)
     * - Used ONLY to get new access tokens, not for API calls
     * 
     * WHY SEPARATE REFRESH TOKENS?
     * - If access token is stolen, it expires in 15 min
     * - Refresh token is stored more securely
     * - Can revoke refresh token without affecting all access tokens
     * 
     * @param subject The user identifier (email)
     * @return The generated refresh token string
     */
    public String generateRefreshToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpirationMs);  // 30 days
        
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(exp)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    // ========================================================================
    // TOKEN VALIDATION AND PARSING
    // ========================================================================
    
    /**
     * Validates a JWT token by checking its signature and expiration.
     * 
     * This method verifies:
     * 1. Token format is correct (three dot-separated parts)
     * 2. Signature matches (token wasn't tampered with)
     * 3. Token hasn't expired
     * 
     * @param token The JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            // parseClaimsJws() does all the validation:
            // - Parses the token
            // - Verifies the signature using our key
            // - Checks expiration time
            // If any check fails, it throws an exception
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // JwtException covers: ExpiredJwtException, MalformedJwtException, 
            // SignatureException, UnsupportedJwtException
            // IllegalArgumentException: null or empty token
            return false;
        }
    }

    /**
     * Extracts the subject (user email) from a JWT token.
     * 
     * IMPORTANT: Only call this after validateToken() returns true!
     * This method assumes the token is valid and will throw if it's not.
     * 
     * @param token The JWT token to parse
     * @return The subject claim (user email)
     * @throws JwtException if token is invalid
     */
    public String getSubject(String token) {
        // Parse token and extract the claims (payload data)
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();  // Get the payload
        
        return claims.getSubject();  // Return the "sub" claim
    }
}