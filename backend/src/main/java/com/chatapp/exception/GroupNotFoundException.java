package com.chatapp.exception;

/**
 * ============================================================================
 * GROUP NOT FOUND EXCEPTION
 * ============================================================================
 * 
 * Thrown when a group cannot be found by its ID.
 * Results in HTTP 404 Not Found response.
 */
public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(String message) {
        super(message);
    }

    public GroupNotFoundException(Long groupId) {
        super(String.format("Group not found with ID: %d", groupId));
    }
}
