package com.chatapp.dto;

import com.chatapp.model.GroupMember;
import com.chatapp.model.GroupRole;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * GROUP MEMBER RESPONSE DTO
 * ============================================================================
 * 
 * Response object for group member details.
 * Used when returning group membership information to clients.
 */
public class GroupMemberResponse {

    private Long id;
    private Long groupId;
    private UserSearchResponse user;
    private GroupRole role;
    private LocalDateTime joinedAt;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public GroupMemberResponse() {}

    // ========================================================================
    // STATIC FACTORY METHOD
    // ========================================================================

    /**
     * Creates a GroupMemberResponse from a GroupMember entity.
     * 
     * @param groupMember The group member entity
     * @return GroupMemberResponse with all relevant data
     */
    public static GroupMemberResponse fromEntity(GroupMember groupMember) {
        GroupMemberResponse response = new GroupMemberResponse();
        response.setId(groupMember.getId());
        response.setGroupId(groupMember.getGroup().getId());
        response.setUser(UserSearchResponse.fromUser(groupMember.getUser()));
        response.setRole(groupMember.getRole());
        response.setJoinedAt(groupMember.getJoinedAt());
        return response;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public UserSearchResponse getUser() { return user; }
    public void setUser(UserSearchResponse user) { this.user = user; }

    public GroupRole getRole() { return role; }
    public void setRole(GroupRole role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
