package com.chatapp.model;

/**
 * ============================================================================
 * CHAT REQUEST STATUS ENUM
 * ============================================================================
 * 
 * Defines the possible states of a chat request.
 * 
 * STATE TRANSITIONS:
 * ------------------
 * PENDING → ACCEPTED (receiver accepts)
 * PENDING → REJECTED (receiver rejects)
 * 
 * Note: ACCEPTED and REJECTED are terminal states.
 */
public enum ChatRequestStatus {
    /**
     * Request is waiting for receiver's response.
     * This is the initial state when a request is created.
     */
    PENDING,
    
    /**
     * Receiver has accepted the chat request.
     * A conversation will be created between the two users.
     */
    ACCEPTED,
    
    /**
     * Receiver has rejected the chat request.
     * No conversation will be created.
     */
    REJECTED
}
