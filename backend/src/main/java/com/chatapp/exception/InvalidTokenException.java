package com.chatapp.exception;

/**
 * ============================================================================
 * INVALID TOKEN EXCEPTION
 * ============================================================================
 * 
 * This exception is thrown when a JWT token is invalid, expired, or not found.
 * It covers all token-related authentication failures.
 * 
 * WHEN IS THIS THROWN?
 * --------------------
 * 1. VERIFICATION TOKEN ISSUES (in AuthService.verifyEmail):
 *    - Token doesn't exist in Redis
 *    - Token has expired (past 24-hour TTL)
 *    - Token format is incorrect
 * 
 * 2. REFRESH TOKEN ISSUES (in AuthService.refresh):
 *    - JWT signature is invalid (tampered with)
 *    - JWT has expired (past 30-day TTL)
 *    - Token not found in Redis (logged out or never existed)
 * 
 * EXAMPLE SCENARIOS:
 * ------------------
 * Scenario 1: User clicks old verification link
 *   → Token expired in Redis → InvalidTokenException
 *   → HTTP 401: "Invalid or expired verification token"
 * 
 * Scenario 2: User tries to refresh with logged-out token
 *   → Token deleted from Redis on logout → InvalidTokenException
 *   → HTTP 401: "Refresh token not found or expired"
 * 
 * Scenario 3: Hacker modifies JWT payload
 *   → JWT signature doesn't match → InvalidTokenException
 *   → HTTP 401: "Invalid refresh token"
 * 
 * WHAT HAPPENS WHEN THROWN?
 * -------------------------
 * 1. GlobalExceptionHandler catches it
 * 2. Returns HTTP 401 Unauthorized:
 *    {
 *      "timestamp": "2026-01-04T10:30:00",
 *      "status": 401,
 *      "error": "Unauthorized",
 *      "message": "Invalid or expired verification token"
 *    }
 * 
 * WHY 401 UNAUTHORIZED?
 * ---------------------
 * HTTP 401 means "authentication required" or "authentication failed."
 * The client needs to re-authenticate (log in again) to get valid tokens.
 * 
 * SECURITY NOTE:
 * --------------
 * We use a generic "invalid token" message for security.
 * We don't specify WHY it's invalid (expired vs not found vs tampered)
 * because that information could help attackers.
 * 
 * @see GlobalExceptionHandler#handleInvalidToken Where this is caught
 * @see AuthService Where this is thrown
 */
public class InvalidTokenException extends RuntimeException {
    
    /**
     * Creates a new InvalidTokenException with a message.
     * 
     * @param message Description of the error (e.g., "Invalid refresh token")
     */
    public InvalidTokenException(String message) {
        super(message);
    }
}