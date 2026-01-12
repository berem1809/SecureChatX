package com.chatapp.exception;

/**
 * ============================================================================
 * RATE LIMIT EXCEEDED EXCEPTION
 * ============================================================================
 * 
 * This exception is thrown when a client makes too many requests in a short
 * time period, exceeding the configured rate limits.
 * 
 * RATE LIMITS IN OUR APPLICATION:
 * --------------------------------
 * | Endpoint           | Limit        | Purpose                          |
 * |--------------------|--------------|----------------------------------|
 * | /api/auth/login    | 5/minute     | Prevent brute-force attacks      |
 * | /api/auth/register | 3/minute     | Prevent spam account creation    |
 * | Other endpoints    | 100/minute   | Prevent general API abuse        |
 * 
 * WHEN IS THIS THROWN?
 * --------------------
 * In RateLimitFilter when bucket.tryConsume(1) returns false:
 * - User's "bucket" of allowed requests is empty
 * - All tokens have been consumed within the time window
 * - Must wait for tokens to refill (happens every minute)
 * 
 * EXAMPLE SCENARIO:
 * -----------------
 * User tries to log in 6 times in 1 minute (limit is 5):
 * 
 * Request 1: ✓ Allowed (4 tokens left)
 * Request 2: ✓ Allowed (3 tokens left)
 * Request 3: ✓ Allowed (2 tokens left)
 * Request 4: ✓ Allowed (1 token left)
 * Request 5: ✓ Allowed (0 tokens left)
 * Request 6: ✗ BLOCKED → RateLimitExceededException
 * 
 * WHAT HAPPENS WHEN THROWN?
 * -------------------------
 * 1. GlobalExceptionHandler catches it
 * 2. Returns HTTP 429 Too Many Requests:
 *    {
 *      "timestamp": "2026-01-04T10:30:00",
 *      "status": 429,
 *      "error": "Too Many Requests",
 *      "message": "Rate limit exceeded. Please try again later."
 *    }
 * 
 * WHY 429 TOO MANY REQUESTS?
 * --------------------------
 * HTTP 429 is the standard status code for rate limiting.
 * It tells the client:
 * - "You've made too many requests"
 * - "Wait before trying again"
 * - (Optional) Retry-After header can specify wait time
 * 
 * WHY RATE LIMITING MATTERS:
 * --------------------------
 * 1. BRUTE-FORCE PROTECTION: Limits password guessing attempts
 * 2. DoS PREVENTION: Stops attackers from overwhelming the server
 * 3. FAIR USAGE: Ensures all users get fair access to resources
 * 4. COST CONTROL: Prevents excessive API usage costs
 * 
 * NOTE:
 * -----
 * Currently, RateLimitFilter returns 429 directly without throwing this
 * exception. This exception class is available for future use or for
 * rate limiting in service layer code.
 * 
 * @see RateLimitFilter Where rate limiting is enforced
 * @see RateLimitConfig Where rate limits are configured
 * @see GlobalExceptionHandler#handleRateLimitExceeded Where this is caught
 */
public class RateLimitExceededException extends RuntimeException {
    
    /**
     * Creates a new RateLimitExceededException with a message.
     * 
     * @param message Description of the error (e.g., "Rate limit exceeded")
     */
    public RateLimitExceededException(String message) {
        super(message);
    }
}
