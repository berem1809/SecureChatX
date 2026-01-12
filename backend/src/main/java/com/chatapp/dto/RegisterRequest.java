package com.chatapp.dto;

/**
 * ============================================================================
 * REGISTER REQUEST DTO (Data Transfer Object)
 * ============================================================================
 * 
 * This class carries registration data from the client to the server.
 * It acts as a "container" for the data sent in the JSON request body.
 * 
 * WHAT IS A DTO?
 * --------------
 * DTO (Data Transfer Object) is a design pattern used to transfer data
 * between different parts of an application or between systems.
 * 
 * WHY USE DTOs?
 * -------------
 * 1. SEPARATION: Keeps API contract separate from database entities
 * 2. SECURITY: Control exactly what data clients can send/receive
 * 3. VALIDATION: Can add validation annotations (@NotNull, @Email, etc.)
 * 4. FLEXIBILITY: Can change internal models without affecting API
 * 
 * HOW IT WORKS:
 * -------------
 * 1. Client sends JSON: {"email":"...", "password":"...", "displayName":"..."}
 * 2. Spring's @RequestBody annotation + Jackson library converts JSON to this object
 * 3. Controller receives a populated RegisterRequest object
 * 4. Service layer uses the data to create a User entity
 * 
 * EXAMPLE JSON REQUEST:
 * {
 *   "email": "user@example.com",
 *   "password": "SecurePass123!",
 *   "displayName": "John Doe"
 * }
 */
public class RegisterRequest {
    
    /**
     * User's email address - used as unique identifier for login.
     * Will be validated for format and uniqueness during registration.
     */
    private String email;
    
    /**
     * User's chosen password - will be hashed with BCrypt before storage.
     * NEVER stored in plain text. Should meet password strength requirements.
     */
    private String password;
    
    /**
     * User's display name - shown to other users in the chat.
     * Optional but recommended for better user experience.
     */
    private String displayName;

    /**
     * Default constructor - required by Jackson for JSON deserialization.
     * Jackson creates an empty object first, then sets fields using setters.
     */
    public RegisterRequest() {}
    
    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================
    // These are required for Jackson to read/write field values.
    // Jackson uses reflection to find getter/setter methods.
    
    /** Gets the email address */
    public String getEmail() { return email; }
    
    /** Sets the email address */
    public void setEmail(String email) { this.email = email; }
    
    /** Gets the password (plain text from client) */
    public String getPassword() { return password; }
    
    /** Sets the password */
    public void setPassword(String password) { this.password = password; }
    
    /** Gets the display name */
    public String getDisplayName() { return displayName; }
    
    /** Sets the display name */
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
