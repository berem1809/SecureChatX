package com.chatapp.exception;

/**
 * ============================================================================
 * CONVERSATION NOT FOUND EXCEPTION
 * ============================================================================
 * 
 * Thrown when a conversation cannot be found by its ID.
 * Results in HTTP 404 Not Found response.
 */
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(String message) {
        super(message);
    }

    public ConversationNotFoundException(Long conversationId) {
        super(String.format("Conversation not found with ID: %d", conversationId));
    }
}
