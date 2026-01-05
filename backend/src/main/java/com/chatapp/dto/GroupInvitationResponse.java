package com.chatapp.dto;

import com.chatapp.model.GroupInvitation;
import com.chatapp.model.GroupInvitationStatus;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * GROUP INVITATION RESPONSE DTO
 * ============================================================================
 * 
 * Response object for group invitation details.
 * Used when returning invitation information to clients.
 */
public class GroupInvitationResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private UserSearchResponse inviter;
    private UserSearchResponse invitee;
    private GroupInvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public GroupInvitationResponse() {}

    // ========================================================================
    // STATIC FACTORY METHOD
    // ========================================================================

    /**
     * Creates a GroupInvitationResponse from a GroupInvitation entity.
     * 
     * @param invitation The invitation entity
     * @return GroupInvitationResponse with all relevant data
     */
    public static GroupInvitationResponse fromEntity(GroupInvitation invitation) {
        GroupInvitationResponse response = new GroupInvitationResponse();
        response.setId(invitation.getId());
        response.setGroupId(invitation.getGroup().getId());
        response.setGroupName(invitation.getGroup().getName());
        response.setInviter(UserSearchResponse.fromUser(invitation.getInviter()));
        response.setInvitee(UserSearchResponse.fromUser(invitation.getInvitee()));
        response.setStatus(invitation.getStatus());
        response.setCreatedAt(invitation.getCreatedAt());
        response.setUpdatedAt(invitation.getUpdatedAt());
        return response;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public UserSearchResponse getInviter() { return inviter; }
    public void setInviter(UserSearchResponse inviter) { this.inviter = inviter; }

    public UserSearchResponse getInvitee() { return invitee; }
    public void setInvitee(UserSearchResponse invitee) { this.invitee = invitee; }

    public GroupInvitationStatus getStatus() { return status; }
    public void setStatus(GroupInvitationStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
