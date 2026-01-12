package com.chatapp.exception;

/**
 * ============================================================================
 * CONVERSATION ALREADY EXISTS EXCEPTION
 * ============================================================================
 * 
 * Thrown when attempting to create a conversation between two users
 * when one already exists.
 * Results in HTTP 409 Conflict response.
 * 
 * NOTE: This should rarely happen in practice since conversations
 * are created through the chat request flow, which includes validation.
 */
public class ConversationAlreadyExistsException extends RuntimeException {

    public ConversationAlreadyExistsException(String message) {
        super(message);
    }

    public ConversationAlreadyExistsException(Long user1Id, Long user2Id) {
        super(String.format("A conversation already exists between users %d and %d", user1Id, user2Id));
    }
}
