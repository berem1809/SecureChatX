package com.chatapp.dto;

/**
 * ============================================================================
 * REFRESH TOKEN REQUEST DTO (Data Transfer Object)
 * ============================================================================
 * 
 * This class carries refresh token data from the client to the server.
 * Used when the client wants to get a new access token without re-logging in.
 * 
 * WHEN IS THIS USED?
 * ------------------
 * When the access token expires (after 15 minutes), the client can:
 * 1. Send the refresh token to /api/auth/refresh
 * 2. Receive a new access token
 * 3. Continue using the API without entering password again
 * 
 * NOTE ON OUR IMPLEMENTATION:
 * ---------------------------
 * In our actual implementation, the refresh token is sent via:
 * - HttpOnly Cookie (preferred, more secure)
 * - OR Request body (fallback for testing)
 * 
 * This DTO is available for the request body approach, but the cookie
 * approach is recommended for production as it's more secure against XSS.
 * 
 * EXAMPLE USAGE:
 * --------------
 * POST /api/auth/refresh
 * Content-Type: application/json
 * {
 *   "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
 * }
 * 
 * SECURITY NOTE:
 * --------------
 * Refresh tokens are sensitive! They can be used to get new access tokens.
 * That's why we prefer HttpOnly cookies over sending in request body.
 */
public class RefreshTokenRequest {
    
    /**
     * The JWT refresh token string.
     * 
     * This is a long-lived token (30 days) that can be exchanged for
     * a new short-lived access token (15 minutes).
     * 
     * Format: eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiw...
     */
    private String refreshToken;

    /**
     * Default no-args constructor.
     * Required by Jackson for JSON deserialization.
     * 
     * Jackson creates object: new RefreshTokenRequest()
     * Then sets fields via: setRefreshToken(value)
     */
    public RefreshTokenRequest() {}

    /**
     * Constructor with refresh token.
     * Useful for creating instances in code (like in tests).
     * 
     * @param refreshToken The JWT refresh token
     */
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Gets the refresh token.
     * 
     * @return The JWT refresh token string
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token.
     * Called by Jackson during JSON deserialization.
     * 
     * @param refreshToken The JWT refresh token string
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}