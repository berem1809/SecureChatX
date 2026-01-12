package com.chatapp.dto;

/**
 * ============================================================================
 * USER SEARCH RESPONSE DTO
 * ============================================================================
 * 
 * Response object for user search results.
 * Contains only safe user information (no passwords, sensitive data).
 * 
 * SECURITY:
 * ---------
 * This DTO deliberately excludes:
 * - Password (even hashed)
 * - Account status
 * - Roles
 * - Any sensitive information
 */
public class UserSearchResponse {

    /**
     * User's unique identifier.
     */
    private Long id;

    /**
     * User's email address.
     * Used for sending chat requests.
     */
    private String email;

    /**
     * User's display name.
     * Shown in search results and chat lists.
     */
    private String displayName;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public UserSearchResponse() {}

    public UserSearchResponse(Long id, String email, String displayName) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
    }

    // ========================================================================
    // STATIC FACTORY METHOD
    // ========================================================================

    /**
     * Creates a UserSearchResponse from a User entity.
     * This ensures we only expose safe data.
     * 
     * @param user The user entity
     * @return UserSearchResponse with safe data only
     */
    public static UserSearchResponse fromUser(com.chatapp.model.User user) {
        return new UserSearchResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName()
        );
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
