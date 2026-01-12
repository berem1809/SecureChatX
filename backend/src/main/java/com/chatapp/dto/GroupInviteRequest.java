package com.chatapp.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ============================================================================
 * GROUP INVITE REQUEST DTO
 * ============================================================================
 * 
 * Request object for inviting a user to a group.
 * Only group admins can send invitations.
 * 
 * NOTE: The inviter (admin) is determined from the JWT token.
 */
public class GroupInviteRequest {

    /**
     * ID of the user to invite to the group.
     */
    @NotNull(message = "Invitee ID is required")
    private Long inviteeId;

    /**
     * Optional message to include with the invitation.
     */
    private String message;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public GroupInviteRequest() {}

    public GroupInviteRequest(Long inviteeId) {
        this.inviteeId = inviteeId;
    }

    public GroupInviteRequest(Long inviteeId, String message) {
        this.inviteeId = inviteeId;
        this.message = message;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getInviteeId() { return inviteeId; }
    public void setInviteeId(Long inviteeId) { this.inviteeId = inviteeId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
