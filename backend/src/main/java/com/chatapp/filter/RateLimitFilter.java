package com.chatapp.filter;

// ============================================================================
// IMPORTS
// ============================================================================

import com.chatapp.config.RateLimitConfig;  // Provides rate limit buckets

// Bucket4j - Token bucket rate limiting library
import io.github.bucket4j.Bucket;  // The bucket that holds tokens

// Jakarta Servlet API (was javax.servlet before Jakarta EE 9)
import jakarta.servlet.FilterChain;       // Chain of filters to execute
import jakarta.servlet.ServletException;  // Filter exception
import jakarta.servlet.http.HttpServletRequest;   // HTTP request
import jakarta.servlet.http.HttpServletResponse;  // HTTP response

// Logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Spring annotations
import org.springframework.core.Ordered;           // For filter ordering
import org.springframework.core.annotation.Order;  // Set filter priority
import org.springframework.http.HttpStatus;        // HTTP status codes
import org.springframework.stereotype.Component;   // Spring component
import org.springframework.web.filter.OncePerRequestFilter;  // Base filter class

import java.io.IOException;

/**
 * ============================================================================
 * RATE LIMIT FILTER - Protects endpoints from abuse
 * ============================================================================
 * 
 * WHAT IS RATE LIMITING?
 * ----------------------
 * Rate limiting restricts how many requests a client can make in a time period.
 * It's essential for:
 * 
 * 1. PREVENTING BRUTE FORCE ATTACKS
 *    - Without: Attacker tries millions of password guesses
 *    - With: After 5 tries per minute, attacker is blocked
 * 
 * 2. PREVENTING DENIAL OF SERVICE (DoS)
 *    - Without: Single client floods server with requests
 *    - With: Excessive requests are rejected, server stays healthy
 * 
 * 3. ENSURING FAIR USAGE
 *    - Without: One user hogs all resources
 *    - With: All users get fair share of server capacity
 * 
 * TOKEN BUCKET ALGORITHM (used by Bucket4j):
 * ------------------------------------------
 * Imagine a bucket that holds tokens:
 * 
 * 1. Bucket starts full of tokens (e.g., 5 tokens)
 * 2. Each request consumes 1 token
 * 3. If no tokens available → request rejected (429 Too Many Requests)
 * 4. Tokens refill over time (e.g., 5 per minute)
 * 
 * EXAMPLE: Login bucket (5 requests per minute)
 * 
 * Time 00:00 - Bucket: [●●●●●] (5 tokens)
 * Time 00:05 - Login 1 → [●●●●○] (4 tokens) ✓ Allowed
 * Time 00:10 - Login 2 → [●●●○○] (3 tokens) ✓ Allowed
 * Time 00:15 - Login 3 → [●●○○○] (2 tokens) ✓ Allowed
 * Time 00:20 - Login 4 → [●○○○○] (1 token)  ✓ Allowed
 * Time 00:25 - Login 5 → [○○○○○] (0 tokens) ✓ Allowed
 * Time 00:30 - Login 6 → [○○○○○] (0 tokens) ✗ REJECTED (429)
 * Time 01:00 - Tokens refill → [●●●●●] (5 tokens again)
 * 
 * OUR RATE LIMITS:
 * ----------------
 * - /api/auth/login:    5 requests per minute per IP
 * - /api/auth/register: 3 requests per minute per IP
 * - Other endpoints:    100 requests per minute per IP
 * 
 * WHY PER-IP TRACKING?
 * --------------------
 * Each IP address gets its own bucket. This way:
 * - Legitimate users aren't affected by attackers
 * - Each user has their own rate limit quota
 * - Attackers can't exhaust limits for everyone
 * 
 * FILTER EXECUTION ORDER:
 * -----------------------
 * @Order(Ordered.HIGHEST_PRECEDENCE) makes this run FIRST.
 * We want to reject excessive requests BEFORE any processing:
 * 
 * Request → [RateLimitFilter] → [JwtFilter] → [SecurityFilter] → Controller
 *              ↑
 *              Rejects early if rate exceeded
 * 
 * @see RateLimitConfig Where buckets are created and configured
 */
@Component  // Marks this as a Spring-managed bean
@Order(Ordered.HIGHEST_PRECEDENCE)  // Run this filter FIRST (before security filters)
public class RateLimitFilter extends OncePerRequestFilter {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Logger for rate limit events (especially violations) */
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    /**
     * RateLimitConfig provides the token buckets for each endpoint type.
     * Buckets are cached by IP address in ConcurrentHashMaps.
     */
    private final RateLimitConfig rateLimitConfig;

    /**
     * Constructor injection - Spring provides the rate limit config.
     */
    public RateLimitFilter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    // ========================================================================
    // FILTER LOGIC
    // ========================================================================

    /**
     * Main filter method - called for every HTTP request.
     * 
     * OncePerRequestFilter guarantees this runs exactly ONCE per request,
     * even if the request is forwarded internally.
     * 
     * FLOW:
     * 1. Extract client IP address
     * 2. Determine request path
     * 3. Get appropriate bucket for path + IP
     * 4. Try to consume 1 token from bucket
     * 5. If token available → allow request
     * 6. If no token → reject with 429
     * 
     * @param request The HTTP request
     * @param response The HTTP response
     * @param filterChain The chain of remaining filters
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Step 1: Get client's IP address (handles proxies)
        String clientIp = getClientIp(request);
        
        // Step 2: Get the request path (e.g., "/api/auth/login")
        String path = request.getRequestURI();
        
        // Step 3: Get the HTTP method for more granular rate limiting
        String method = request.getMethod();
        
        // Step 4: Get the appropriate bucket for this path + IP + method
        // Different endpoints have different rate limits
        Bucket bucket = getBucketForPath(path, clientIp, method);
        
        // Step 5: Try to consume 1 token from the bucket
        // tryConsume(1) is atomic and thread-safe
        if (bucket.tryConsume(1)) {
            // Token consumed successfully - request is allowed
            // Continue to next filter in chain
            filterChain.doFilter(request, response);
        } else {
            // No tokens available - rate limit exceeded!
            logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            
            // Return HTTP 429 Too Many Requests
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());  // 429
            response.setContentType("application/json");
            
            // JSON error response
            response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
            
            // Don't call filterChain.doFilter() - request stops here!
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Gets the appropriate rate limit bucket for a request path.
     * 
     * Different endpoints have different security needs:
     * - Login: Low limit (5/min) - prevent brute force
     * - Register: Low limit (3/min) - prevent spam accounts
     * - Chat requests: Moderate (20/min) - prevent spam
     * - Group invitations: Moderate (20/min) - prevent invitation spam
     * - User search: Higher (30/min) - common operation
     * - General: Higher limit (100/min) - normal usage
     * 
     * @param path The request URI (e.g., "/api/auth/login")
     * @param clientIp The client's IP address
     * @param method The HTTP method (GET, POST, etc.)
     * @return The Bucket for rate limiting
     */
    private Bucket getBucketForPath(String path, String clientIp, String method) {
        // Check if it's a modifying request (POST, PUT, DELETE)
        boolean isModifyingRequest = "POST".equalsIgnoreCase(method) || 
                                     "PUT".equalsIgnoreCase(method) || 
                                     "DELETE".equalsIgnoreCase(method);
        
        if (path.contains("/api/auth/login")) {
            // Login endpoint: 5 requests per minute
            // Prevents password brute force attacks
            return rateLimitConfig.getLoginBucket(clientIp);
            
        } else if (path.contains("/api/auth/register")) {
            // Register endpoint: 3 requests per minute
            // Prevents spam account creation
            return rateLimitConfig.getRegisterBucket(clientIp);
            
        } else if (path.contains("/api/chat-requests") && isModifyingRequest) {
            // Chat request creation: 20 requests per minute
            // Prevents spam chat requests
            return rateLimitConfig.getChatRequestBucket(clientIp);
            
        } else if (path.contains("/api/groups") && path.contains("/invitations") && isModifyingRequest) {
            // Group invitation creation: 20 requests per minute
            // Prevents invitation spam
            return rateLimitConfig.getGroupInvitationBucket(clientIp);
            
        } else if (path.contains("/api/users/search")) {
            // User search: 30 requests per minute
            // Common operation but still needs protection
            return rateLimitConfig.getUserSearchBucket(clientIp);
            
        } else {
            // All other endpoints: 100 requests per minute
            // Allows normal usage while preventing abuse
            return rateLimitConfig.getGeneralBucket(clientIp);
        }
    }

    /**
     * Extracts the real client IP address from the request.
     * 
     * WHY IS THIS COMPLICATED?
     * When your app is behind a proxy or load balancer:
     * 
     * User (IP: 1.2.3.4) → Nginx Proxy (IP: 10.0.0.1) → Your App
     * 
     * Without special handling:
     * - request.getRemoteAddr() returns 10.0.0.1 (proxy's IP)
     * - All users appear to have same IP!
     * - Rate limits would be shared (bad!)
     * 
     * With X-Forwarded-For header:
     * - Proxy adds: X-Forwarded-For: 1.2.3.4
     * - We read the header to get real IP
     * - Each user gets their own rate limit
     * 
     * HEADER PRIORITY:
     * 1. X-Forwarded-For (standard, can have multiple IPs)
     * 2. X-Real-IP (Nginx-specific, single IP)
     * 3. request.getRemoteAddr() (fallback, direct connection)
     * 
     * @param request The HTTP request
     * @return The client's real IP address
     */
    private String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For first (most common proxy header)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs if multiple proxies:
            // "client, proxy1, proxy2" - we want the first one (client)
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP (used by Nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fallback: direct connection (no proxy)
        return request.getRemoteAddr();
    }
    
    /*
     * SECURITY NOTES:
     * ===============
     * 
     * 1. IP SPOOFING RISK:
     *    X-Forwarded-For can be spoofed by clients!
     *    Only trust it if your proxy is configured to overwrite it.
     *    In production, configure your proxy to strip/set these headers.
     * 
     * 2. SHARED IPs (NAT):
     *    Many users behind same corporate/university network share IP.
     *    They'll share rate limits. Consider:
     *    - User-based rate limiting (after authentication)
     *    - API keys for higher limits
     * 
     * 3. DISTRIBUTED ATTACKS:
     *    Attackers can use many IPs (botnet).
     *    Per-IP rate limiting alone isn't enough.
     *    Consider:
     *    - Global rate limits
     *    - WAF (Web Application Firewall)
     *    - Cloudflare/AWS WAF
     */
}
