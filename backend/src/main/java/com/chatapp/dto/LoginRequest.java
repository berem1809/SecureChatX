package com.chatapp.dto;

/**
 * ============================================================================
 * LOGIN REQUEST DTO (Data Transfer Object)
 * ============================================================================
 * 
 * This class carries login credentials from the client to the server.
 * Used when a user wants to authenticate and receive JWT tokens.
 * 
 * SECURITY FLOW:
 * --------------
 * 1. Client sends email + password over HTTPS (encrypted in transit)
 * 2. Server receives this DTO with the credentials
 * 3. Server looks up user by email in MySQL database
 * 4. Server compares password with BCrypt hash (never stores plain password)
 * 5. If match, server generates JWT tokens and returns them
 * 6. If no match, server returns 401 Unauthorized
 * 
 * EXAMPLE JSON REQUEST:
 * POST /api/auth/login
 * Content-Type: application/json
 * {
 *   "email": "user@example.com",
 *   "password": "SecurePass123!"
 * }
 * 
 * IMPORTANT SECURITY NOTES:
 * -------------------------
 * - Password is only used for comparison, never stored in this object long-term
 * - Always use HTTPS to protect credentials in transit
 * - Implement rate limiting to prevent brute-force attacks
 * - Don't reveal whether email exists in error messages (prevents enumeration)
 */
public class LoginRequest {
    
    /**
     * User's email address used to identify their account.
     * This is the "username" in our authentication system.
     */
    private String email;
    
    /**
     * User's password in plain text (as typed by user).
     * Will be compared against the BCrypt hash stored in the database.
     * This value should NEVER be logged or stored.
     */
    private String password;

    /**
     * Default constructor required by Jackson for JSON deserialization.
     */
    public LoginRequest() {}
    
    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================
    
    /** Gets the email address */
    public String getEmail() { return email; }
    
    /** Sets the email address */
    public void setEmail(String email) { this.email = email; }
    
    /** Gets the password (plain text) */
    public String getPassword() { return password; }
    
    /** Sets the password */
    public void setPassword(String password) { this.password = password; }
}
