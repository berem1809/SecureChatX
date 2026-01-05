package com.chatapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * ============================================================================
 * GROUP INVITATION ACTION REQUEST DTO
 * ============================================================================
 * 
 * Request object for accepting or rejecting a group invitation.
 * 
 * USAGE:
 * ------
 * POST /api/groups/invitations/{invitationId}/action
 * {
 *   "action": "ACCEPT"  // or "REJECT"
 * }
 */
public class GroupInvitationActionRequest {

    /**
     * The action to perform on the invitation.
     * Valid values: ACCEPT, REJECT
     */
    @NotNull(message = "Action is required")
    @Pattern(regexp = "ACCEPT|REJECT", message = "Action must be ACCEPT or REJECT")
    private String action;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public GroupInvitationActionRequest() {}

    public GroupInvitationActionRequest(String action) {
        this.action = action;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    public boolean isAccept() {
        return "ACCEPT".equalsIgnoreCase(action);
    }

    public boolean isReject() {
        return "REJECT".equalsIgnoreCase(action);
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
