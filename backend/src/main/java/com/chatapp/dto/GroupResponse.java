package com.chatapp.dto;

import com.chatapp.model.Group;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * GROUP RESPONSE DTO
 * ============================================================================
 * 
 * Response object for group details.
 * Used when returning group information to clients.
 */
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private UserSearchResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private int memberCount;
    private List<GroupMemberResponse> members;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public GroupResponse() {}

    // ========================================================================
    // STATIC FACTORY METHODS
    // ========================================================================

    /**
     * Creates a GroupResponse from a Group entity (basic info only).
     * 
     * @param group The group entity
     * @return GroupResponse with basic data
     */
    public static GroupResponse fromEntity(Group group) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        response.setCreatedBy(UserSearchResponse.fromUser(group.getCreatedBy()));
        response.setCreatedAt(group.getCreatedAt());
        response.setLastMessageAt(group.getLastMessageAt());
        response.setMemberCount(group.getMembers().size());
        return response;
    }

    /**
     * Creates a GroupResponse from a Group entity with full member details.
     * 
     * @param group The group entity
     * @return GroupResponse with members included
     */
    public static GroupResponse fromEntityWithMembers(Group group) {
        GroupResponse response = fromEntity(group);
        response.setMembers(
            group.getMembers().stream()
                .map(GroupMemberResponse::fromEntity)
                .collect(Collectors.toList())
        );
        return response;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UserSearchResponse getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserSearchResponse createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public List<GroupMemberResponse> getMembers() { return members; }
    public void setMembers(List<GroupMemberResponse> members) { this.members = members; }
}
