package com.chatapp.dto;

/**
 * ============================================================================
 * AUTH RESPONSE DTO (Data Transfer Object)
 * ============================================================================
 * 
 * This class carries authentication tokens from the server back to the client.
 * Returned after successful login or token refresh operations.
 * 
 * TWO-TOKEN STRATEGY:
 * -------------------
 * We use two different tokens for security:
 * 
 * 1. ACCESS TOKEN (accessToken):
 *    - Short-lived: 15 minutes
 *    - Used for: Every API request (in Authorization header)
 *    - Stored: In memory (JavaScript variable) or localStorage
 *    - If stolen: Limited damage due to short expiry
 *    - Contains: User email, roles, expiration time
 * 
 * 2. REFRESH TOKEN (refreshToken):
 *    - Long-lived: 30 days
 *    - Used for: Getting new access tokens (only)
 *    - Stored: HttpOnly cookie (can't be accessed by JavaScript)
 *    - Also stored: Hashed in Redis (for server-side validation)
 *    - If stolen: Can be revoked by deleting from Redis
 * 
 * EXAMPLE LOGIN RESPONSE:
 * -----------------------
 * HTTP Response:
 * {
 *   "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiw..."
 * }
 * + Set-Cookie: refreshToken=eyJhbGciOiJIUzUxMiJ9...; HttpOnly; Secure; Path=/
 * 
 * Note: refreshToken is set to null in the response body because it's sent via cookie instead.
 * 
 * HOW CLIENT USES TOKENS:
 * -----------------------
 * 1. After login, store accessToken in memory
 * 2. For API calls, add header: Authorization: Bearer <accessToken>
 * 3. When accessToken expires (401 response), call /api/auth/refresh
 * 4. Refresh endpoint uses cookie automatically (browser sends it)
 * 5. Receive new accessToken, continue making API calls
 */
public class AuthResponse {
    
    /**
     * JWT Access Token - used for authenticating API requests.
     * 
     * Format: eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOi...
     * Contains: User email (subject), roles, issue time, expiration
     * Validity: 15 minutes from creation
     * 
     * Client should:
     * - Store this in memory (not localStorage for XSS protection)
     * - Include in every API request: Authorization: Bearer <token>
     * - Request new token when this expires
     */
    private String accessToken;
    
    /**
     * JWT Refresh Token - used for obtaining new access tokens.
     * 
     * SECURITY NOTE: In our implementation, this is usually NULL in the 
     * response body because we send it via HttpOnly cookie instead.
     * This prevents XSS attacks from stealing the refresh token.
     * 
     * This field is populated during login/refresh operations internally,
     * then set to null before sending response to client.
     */
    private String refreshToken;

    /**
     * Default constructor required by Jackson for JSON serialization.
     */
    public AuthResponse() {}
    
    /**
     * Constructor with both tokens - used by AuthService when creating response.
     * 
     * @param accessToken The JWT access token
     * @param refreshToken The JWT refresh token (will be moved to cookie)
     */
    public AuthResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    
    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================
    
    /** Gets the access token */
    public String getAccessToken() { return accessToken; }
    
    /** Sets the access token */
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    /** Gets the refresh token */
    public String getRefreshToken() { return refreshToken; }
    
    /**
     * Sets the refresh token.
     * NOTE: Usually called with null to remove from response body
     * since we send it via HttpOnly cookie instead.
     */
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
