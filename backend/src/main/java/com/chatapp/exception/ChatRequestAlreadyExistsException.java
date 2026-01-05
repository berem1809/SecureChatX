package com.chatapp.exception;

/**
 * ============================================================================
 * CHAT REQUEST ALREADY EXISTS EXCEPTION
 * ============================================================================
 * 
 * Thrown when a user attempts to send a chat request to someone
 * when a request already exists between them.
 * Results in HTTP 409 Conflict response.
 * 
 * EXAMPLE SCENARIOS:
 * ------------------
 * - User A sends request to User B, then tries to send another
 * - User A tries to send request to User B, but User B already sent one to A
 */
public class ChatRequestAlreadyExistsException extends RuntimeException {

    public ChatRequestAlreadyExistsException(String message) {
        super(message);
    }

    public ChatRequestAlreadyExistsException(Long senderId, Long receiverId) {
        super(String.format("A chat request already exists between users %d and %d", senderId, receiverId));
    }
}
