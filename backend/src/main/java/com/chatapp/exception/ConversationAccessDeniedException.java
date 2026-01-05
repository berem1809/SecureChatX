package com.chatapp.exception;

/**
 * ============================================================================
 * CONVERSATION ACCESS DENIED EXCEPTION
 * ============================================================================
 * 
 * Thrown when a user attempts to access a conversation they are not a member of.
 * Results in HTTP 403 Forbidden response.
 * 
 * EXAMPLE SCENARIOS:
 * ------------------
 * - User tries to read messages from a conversation they're not part of
 * - User tries to send a message to a conversation they're not part of
 * - User tries to get details of a conversation they're not part of
 */
public class ConversationAccessDeniedException extends RuntimeException {

    public ConversationAccessDeniedException(String message) {
        super(message);
    }

    public ConversationAccessDeniedException(Long conversationId, Long userId) {
        super(String.format("User %d is not a participant of conversation %d", userId, conversationId));
    }
}
