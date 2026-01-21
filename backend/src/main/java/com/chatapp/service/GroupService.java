package com.chatapp.service;

import com.chatapp.dto.GroupCreateRequest;
import com.chatapp.dto.GroupMemberResponse;
import com.chatapp.dto.GroupResponse;
import com.chatapp.exception.*;
import com.chatapp.model.*;
import com.chatapp.repository.*;
import com.chatapp.util.GroupPermissionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * GROUP SERVICE - Handles group chat operations
 * ============================================================================
 * 
 * Manages group lifecycle:
 * 1. Creating groups
 * 2. Managing members (add, remove, promote)
 * 3. Retrieving group information
 * 
 * BUSINESS RULES:
 * ---------------
 * - Group creator automatically becomes admin
 * - Only admins can invite members
 * - Only admins can remove members
 * - Only admins can promote members to admin
 * - A group must have at least one admin
 */
public interface GroupService {
    
    /**
     * Creates a new group.
     * The creator automatically becomes an admin.
     * 
     * @param creatorId The ID of the user creating the group
     * @param request The group creation request
     * @return The created group
     */
    GroupResponse createGroup(Long creatorId, GroupCreateRequest request);
    
    /**
     * Gets all groups where the user is a member.
     * 
     * @param userId The user's ID
     * @return List of groups
     */
    List<GroupResponse> getUserGroups(Long userId);
    
    /**
     * Gets a specific group by ID.
     * Validates that the requesting user is a member.
     * 
     * @param groupId The group ID
     * @param userId The ID of the user requesting
     * @return The group with member details
     */
    GroupResponse getGroup(Long groupId, Long userId);
    
    /**
     * Gets all members of a group.
     * 
     * @param groupId The group ID
     * @param userId The ID of the user requesting (must be a member)
     * @return List of group members
     */
    List<GroupMemberResponse> getGroupMembers(Long groupId, Long userId);
    
    /**
     * Removes a member from a group (admin only).
     * 
     * @param groupId The group ID
     * @param adminUserId The ID of the admin performing the action
     * @param memberUserId The ID of the member to remove
     */
    void removeMember(Long groupId, Long adminUserId, Long memberUserId);
    
    /**
     * Promotes a member to admin (admin only).
     * 
     * @param groupId The group ID
     * @param adminUserId The ID of the admin performing the action
     * @param memberUserId The ID of the member to promote
     * @return The updated member info
     */
    GroupMemberResponse promoteMember(Long groupId, Long adminUserId, Long memberUserId);
    
    /**
     * Allows a member to leave a group.
     * 
     * BUSINESS RULES:
     * ---------------
     * 1. Regular members can always leave
     * 2. Admin can only leave if:
     *    a) There are no other members (admin deletes group and exits)
     *    b) Another user has been promoted to admin first
     * 3. When admin is the only member left:
     *    - Group is automatically deleted
     *    - All group data is removed (cascade delete)
     *    - No further access is possible
     * 
     * @param groupId The group ID
     * @param userId The ID of the user leaving
     * @throws RuntimeException if admin tries to leave with other members still active
     */
    void leaveGroup(Long groupId, Long userId);
    
    /**
     * Updates group information (admin only).
     * 
     * @param groupId The group ID
     * @param adminUserId The ID of the admin performing the action
     * @param name New group name (optional)
     * @param description New group description (optional)
     * @return The updated group
     */
    GroupResponse updateGroup(Long groupId, Long adminUserId, String name, String description);
}

/**
 * Implementation of GroupService.
 */
@Service
class GroupServiceImpl implements GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupPermissionValidator permissionValidator;

    public GroupServiceImpl(GroupRepository groupRepository,
                            GroupMemberRepository groupMemberRepository,
                            UserRepository userRepository,
                            GroupPermissionValidator permissionValidator) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.permissionValidator = permissionValidator;
    }

    @Override
    @Transactional
    public GroupResponse createGroup(Long creatorId, GroupCreateRequest request) {
        logger.info("User {} creating group: {}", creatorId, request.getName());
        
        // Validate group name
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Group name cannot be empty");
        }
        
        // Get creator
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new UserNotFoundException(creatorId));
        
        // Create group
        Group group = new Group(request.getName(), request.getDescription(), creator);
        Group savedGroup = groupRepository.save(group);
        
        // Add creator as admin
        GroupMember adminMember = new GroupMember(savedGroup, creator, GroupRole.ADMIN);
        groupMemberRepository.save(adminMember);
        
        // Update the group's members list
        savedGroup.getMembers().add(adminMember);
        
        logger.info("Group created with ID: {}", savedGroup.getId());
        return GroupResponse.fromEntityWithMembers(savedGroup);
    }

    @Override
    public List<GroupResponse> getUserGroups(Long userId) {
        logger.debug("Getting groups for user {}", userId);
        
        return groupRepository.findGroupsByMemberId(userId).stream()
            .map(GroupResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public GroupResponse getGroup(Long groupId, Long userId) {
        logger.debug("Getting group {} for user {}", groupId, userId);
        
        // Validate membership
        permissionValidator.validateMember(groupId, userId);
        
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new GroupNotFoundException(groupId));
        
        return GroupResponse.fromEntityWithMembers(group);
    }

    @Override
    public List<GroupMemberResponse> getGroupMembers(Long groupId, Long userId) {
        logger.debug("Getting members of group {} for user {}", groupId, userId);
        
        // Validate membership
        permissionValidator.validateMember(groupId, userId);
        
        return groupMemberRepository.findByGroupId(groupId).stream()
            .map(GroupMemberResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long adminUserId, Long memberUserId) {
        logger.info("Admin {} removing member {} from group {}", adminUserId, memberUserId, groupId);
        
        // Validate admin permission
        permissionValidator.validateAdmin(groupId, adminUserId);
        
        // Check if target is a member
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, memberUserId)
            .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        // Cannot remove the last admin
        if (member.isAdmin() && groupMemberRepository.countAdminsByGroupId(groupId) <= 1) {
            throw new RuntimeException("Cannot remove the last admin of the group");
        }
        
        // Remove member
        groupMemberRepository.delete(member);
        
        logger.info("Member {} removed from group {}", memberUserId, groupId);
    }

    @Override
    @Transactional
    public GroupMemberResponse promoteMember(Long groupId, Long adminUserId, Long memberUserId) {
        logger.info("Admin {} promoting member {} in group {}", adminUserId, memberUserId, groupId);
        
        // Validate admin permission
        permissionValidator.validateAdmin(groupId, adminUserId);
        
        // Get member
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, memberUserId)
            .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        // Check if already admin
        if (member.isAdmin()) {
            throw new RuntimeException("User is already an admin");
        }
        
        // Promote to admin
        member.setRole(GroupRole.ADMIN);
        GroupMember savedMember = groupMemberRepository.save(member);
        
        logger.info("Member {} promoted to admin in group {}", memberUserId, groupId);
        return GroupMemberResponse.fromEntity(savedMember);
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        logger.info("User {} leaving group {}", userId, groupId);
        
        // Get member
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new GroupAccessDeniedException(groupId, userId));
        
        // Check if member is admin
        if (member.isAdmin()) {
            long adminCount = groupMemberRepository.countAdminsByGroupId(groupId);
            long memberCount = groupMemberRepository.countByGroupId(groupId);
            
            // Admin cannot leave if there are other members and no other admins
            if (adminCount <= 1 && memberCount > 1) {
                throw new RuntimeException(
                    "Admin cannot leave group while other members are active. " +
                    "Please promote another member to admin first, or remove all other members."
                );
            }
            
            // If admin is the last member, delete the group
            if (memberCount == 1) {
                groupRepository.deleteById(groupId);
                logger.info("Group {} deleted (admin left and was the last member)", groupId);
                return;
            }
        }
        
        // Remove member from group
        groupMemberRepository.delete(member);
        logger.info("User {} left group {}", userId, groupId);
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Long groupId, Long adminUserId, String name, String description) {
        logger.info("Admin {} updating group {}", adminUserId, groupId);
        
        // Validate admin permission
        Group group = permissionValidator.validateAdmin(groupId, adminUserId);
        
        // Update fields
        if (name != null && !name.isBlank()) {
            group.setName(name);
        }
        if (description != null) {
            group.setDescription(description);
        }
        
        Group savedGroup = groupRepository.save(group);
        
        logger.info("Group {} updated", groupId);
        return GroupResponse.fromEntity(savedGroup);
    }
}
