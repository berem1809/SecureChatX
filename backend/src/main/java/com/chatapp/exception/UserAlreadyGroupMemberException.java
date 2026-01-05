package com.chatapp.exception;

/**
 * ============================================================================
 * USER ALREADY GROUP MEMBER EXCEPTION
 * ============================================================================
 * 
 * Thrown when attempting to invite a user who is already a member of the group.
 * Results in HTTP 409 Conflict response.
 */
public class UserAlreadyGroupMemberException extends RuntimeException {

    public UserAlreadyGroupMemberException(String message) {
        super(message);
    }

    public UserAlreadyGroupMemberException(Long userId, Long groupId) {
        super(String.format("User %d is already a member of group %d", userId, groupId));
    }
}
