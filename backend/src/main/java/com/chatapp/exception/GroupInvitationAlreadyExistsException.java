package com.chatapp.exception;

/**
 * ============================================================================
 * GROUP INVITATION ALREADY EXISTS EXCEPTION
 * ============================================================================
 * 
 * Thrown when attempting to invite a user to a group when
 * they already have a pending invitation.
 * Results in HTTP 409 Conflict response.
 */
public class GroupInvitationAlreadyExistsException extends RuntimeException {

    public GroupInvitationAlreadyExistsException(String message) {
        super(message);
    }

    public GroupInvitationAlreadyExistsException(Long groupId, Long inviteeId) {
        super(String.format("User %d already has a pending invitation to group %d", inviteeId, groupId));
    }
}
