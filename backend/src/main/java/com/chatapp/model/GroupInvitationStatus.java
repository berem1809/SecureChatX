package com.chatapp.model;

/**
 * ============================================================================
 * GROUP INVITATION STATUS ENUM
 * ============================================================================
 * 
 * Defines the possible states of a group invitation.
 * 
 * STATE TRANSITIONS:
 * ------------------
 * PENDING → ACCEPTED (invitee accepts)
 * PENDING → REJECTED (invitee rejects)
 * 
 * Note: ACCEPTED and REJECTED are terminal states.
 */
public enum GroupInvitationStatus {
    /**
     * Invitation is waiting for invitee's response.
     * This is the initial state when an invitation is created.
     */
    PENDING,
    
    /**
     * Invitee has accepted the invitation.
     * A GroupMember record will be created.
     */
    ACCEPTED,
    
    /**
     * Invitee has rejected the invitation.
     * No membership will be created.
     */
    REJECTED
}
