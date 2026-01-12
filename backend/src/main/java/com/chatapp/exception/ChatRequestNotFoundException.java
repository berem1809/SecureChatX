package com.chatapp.exception;

/**
 * ============================================================================
 * CHAT REQUEST NOT FOUND EXCEPTION
 * ============================================================================
 * 
 * Thrown when a chat request cannot be found by its ID.
 * Results in HTTP 404 Not Found response.
 */
public class ChatRequestNotFoundException extends RuntimeException {

    public ChatRequestNotFoundException(String message) {
        super(message);
    }

    public ChatRequestNotFoundException(Long requestId) {
        super(String.format("Chat request not found with ID: %d", requestId));
    }
}
