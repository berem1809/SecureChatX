package com.chatapp.util;

import com.chatapp.exception.GroupAccessDeniedException;
import com.chatapp.exception.GroupNotFoundException;
import com.chatapp.model.Group;
import com.chatapp.model.GroupMember;
import com.chatapp.model.GroupRole;
import com.chatapp.repository.GroupMemberRepository;
import com.chatapp.repository.GroupRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * ============================================================================
 * GROUP PERMISSION VALIDATOR
 * ============================================================================
 * 
 * Utility class for validating group access and permissions.
 * Used by service layer to enforce role-based authorization rules.
 * 
 * AUTHORIZATION RULES:
 * --------------------
 * 1. Only group members can access the group
 * 2. Only admins can invite new members
 * 3. Only admins can remove members
 * 4. Only admins can promote members
 * 5. User ID is extracted from JWT token, not from request body
 */
@Component
public class GroupPermissionValidator {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupPermissionValidator(GroupRepository groupRepository, 
                                     GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    /**
     * Validates that a user is a member of a group.
     * 
     * @param groupId The group ID
     * @param userId The user ID to validate
     * @return The Group if user is a member
     * @throws GroupNotFoundException if group doesn't exist
     * @throws GroupAccessDeniedException if user is not a member
     */
    public Group validateMember(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new GroupNotFoundException(groupId));
        
        if (!isMember(groupId, userId)) {
            throw new GroupAccessDeniedException(groupId, userId);
        }
        
        return group;
    }

    /**
     * Validates that a user is an admin of a group.
     * 
     * @param groupId The group ID
     * @param userId The user ID to validate
     * @return The Group if user is an admin
     * @throws GroupNotFoundException if group doesn't exist
     * @throws GroupAccessDeniedException if user is not an admin
     */
    public Group validateAdmin(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new GroupNotFoundException(groupId));
        
        if (!isAdmin(groupId, userId)) {
            throw GroupAccessDeniedException.insufficientRole(groupId, userId, "ADMIN");
        }
        
        return group;
    }

    /**
     * Validates membership and returns the group member record.
     * 
     * @param groupId The group ID
     * @param userId The user ID to validate
     * @return The GroupMember if user is a member
     * @throws GroupAccessDeniedException if user is not a member
     */
    public GroupMember validateAndGetMembership(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new GroupAccessDeniedException(groupId, userId));
    }

    /**
     * Checks if a user is a member of a group.
     * 
     * @param groupId The group ID
     * @param userId The user ID
     * @return true if user is a member
     */
    public boolean isMember(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    /**
     * Checks if a user is an admin of a group.
     * 
     * @param groupId The group ID
     * @param userId The user ID
     * @return true if user is an admin
     */
    public boolean isAdmin(Long groupId, Long userId) {
        return groupMemberRepository.isAdmin(groupId, userId);
    }

    /**
     * Gets the user's role in a group.
     * 
     * @param groupId The group ID
     * @param userId The user ID
     * @return Optional containing the role if user is a member
     */
    public Optional<GroupRole> getUserRole(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .map(GroupMember::getRole);
    }

    /**
     * Checks if a user can perform admin actions on another member.
     * An admin cannot remove or demote themselves if they're the only admin.
     * 
     * @param groupId The group ID
     * @param actorUserId The user performing the action
     * @param targetUserId The user being acted upon
     * @return true if action is allowed
     */
    public boolean canManageMember(Long groupId, Long actorUserId, Long targetUserId) {
        // Must be an admin to manage members
        if (!isAdmin(groupId, actorUserId)) {
            return false;
        }
        
        // Cannot manage yourself if you're the only admin
        if (actorUserId.equals(targetUserId)) {
            long adminCount = groupMemberRepository.countAdminsByGroupId(groupId);
            return adminCount > 1;
        }
        
        return true;
    }
}
