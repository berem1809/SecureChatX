package com.chatapp.exception;

/**
 * ============================================================================
 * USER NOT FOUND EXCEPTION
 * ============================================================================
 * 
 * Thrown when a user cannot be found by ID, email, or username.
 * Results in HTTP 404 Not Found response.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long userId) {
        super(String.format("User not found with ID: %d", userId));
    }
    
    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException(String.format("User not found with email: %s", email));
    }
}
