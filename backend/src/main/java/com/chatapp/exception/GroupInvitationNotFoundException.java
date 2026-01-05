package com.chatapp.exception;

/**
 * ============================================================================
 * GROUP INVITATION NOT FOUND EXCEPTION
 * ============================================================================
 * 
 * Thrown when a group invitation cannot be found by its ID.
 * Results in HTTP 404 Not Found response.
 */
public class GroupInvitationNotFoundException extends RuntimeException {

    public GroupInvitationNotFoundException(String message) {
        super(message);
    }

    public GroupInvitationNotFoundException(Long invitationId) {
        super(String.format("Group invitation not found with ID: %d", invitationId));
    }
}
