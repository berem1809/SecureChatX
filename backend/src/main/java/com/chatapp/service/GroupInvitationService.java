package com.chatapp.service;

import com.chatapp.dto.GroupInvitationResponse;
import com.chatapp.dto.GroupInviteRequest;
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
 * GROUP INVITATION SERVICE - Handles group invitation operations
 * ============================================================================
 * 
 * Manages the invitation workflow:
 * 1. Sending invitations (admin only)
 * 2. Accepting invitations (invitee only)
 * 3. Rejecting invitations (invitee only)
 * 4. Listing pending invitations
 * 
 * BUSINESS RULES:
 * ---------------
 * - Only group admins can send invitations
 * - Only pending invitations can be accepted/rejected
 * - Only the invitee can accept/reject
 * - Duplicate invitations are not allowed
 * - Cannot invite existing members
 */
public interface GroupInvitationService {
    
    /**
     * Creates a new group invitation.
     * Only admins can invite users.
     * 
     * @param groupId The group ID
     * @param inviterId The ID of the admin sending the invitation
     * @param request The invitation request
     * @return The created invitation
     */
    GroupInvitationResponse createInvitation(Long groupId, Long inviterId, GroupInviteRequest request);
    
    /**
     * Accepts a group invitation.
     * The invitee becomes a member of the group.
     * 
     * @param invitationId The invitation ID
     * @param userId The ID of the user accepting (must be invitee)
     * @return The updated invitation
     */
    GroupInvitationResponse acceptInvitation(Long invitationId, Long userId);
    
    /**
     * Rejects a group invitation.
     * 
     * @param invitationId The invitation ID
     * @param userId The ID of the user rejecting (must be invitee)
     * @return The updated invitation
     */
    GroupInvitationResponse rejectInvitation(Long invitationId, Long userId);
    
    /**
     * Gets all pending invitations for a user.
     * 
     * @param userId The user's ID
     * @return List of pending invitations
     */
    List<GroupInvitationResponse> getPendingInvitations(Long userId);
    
    /**
     * Gets all invitations for a group (admin only).
     * 
     * @param groupId The group ID
     * @param adminUserId The admin's user ID
     * @return List of invitations
     */
    List<GroupInvitationResponse> getGroupInvitations(Long groupId, Long adminUserId);
    
    /**
     * Gets a specific invitation by ID.
     * 
     * @param invitationId The invitation ID
     * @param userId The ID of the user requesting (must be inviter or invitee)
     * @return The invitation
     */
    GroupInvitationResponse getInvitation(Long invitationId, Long userId);
    
    /**
     * Cancels a pending invitation (admin only).
     * 
     * @param invitationId The invitation ID
     * @param adminUserId The admin's user ID
     */
    void cancelInvitation(Long invitationId, Long adminUserId);
}

/**
 * Implementation of GroupInvitationService.
 */
@Service
class GroupInvitationServiceImpl implements GroupInvitationService {

    private static final Logger logger = LoggerFactory.getLogger(GroupInvitationServiceImpl.class);

    private final GroupInvitationRepository invitationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupPermissionValidator permissionValidator;

    public GroupInvitationServiceImpl(GroupInvitationRepository invitationRepository,
                                       GroupMemberRepository groupMemberRepository,
                                       GroupRepository groupRepository,
                                       UserRepository userRepository,
                                       GroupPermissionValidator permissionValidator) {
        this.invitationRepository = invitationRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.permissionValidator = permissionValidator;
    }

    @Override
    @Transactional
    public GroupInvitationResponse createInvitation(Long groupId, Long inviterId, GroupInviteRequest request) {
        Long inviteeId = request.getInviteeId();
        
        logger.info("Admin {} inviting user {} to group {}", inviterId, inviteeId, groupId);
        
        // Validate admin permission
        Group group = permissionValidator.validateAdmin(groupId, inviterId);
        
        // Cannot invite yourself
        if (inviterId.equals(inviteeId)) {
            throw new RuntimeException("Cannot invite yourself to a group");
        }
        
        // Get inviter and invitee
        User inviter = userRepository.findById(inviterId)
            .orElseThrow(() -> new UserNotFoundException(inviterId));
        User invitee = userRepository.findById(inviteeId)
            .orElseThrow(() -> new UserNotFoundException(inviteeId));
        
        // Check if user is already a member
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, inviteeId)) {
            throw new UserAlreadyGroupMemberException(inviteeId, groupId);
        }
        
        // Check if pending invitation already exists
        if (invitationRepository.existsPendingByGroupIdAndInviteeId(groupId, inviteeId)) {
            throw new GroupInvitationAlreadyExistsException(groupId, inviteeId);
        }
        
        // Create invitation
        GroupInvitation invitation = new GroupInvitation(group, inviter, invitee);
        GroupInvitation savedInvitation = invitationRepository.save(invitation);
        
        logger.info("Invitation created with ID: {}", savedInvitation.getId());
        return GroupInvitationResponse.fromEntity(savedInvitation);
    }

    @Override
    @Transactional
    public GroupInvitationResponse acceptInvitation(Long invitationId, Long userId) {
        logger.info("User {} accepting invitation {}", userId, invitationId);
        
        GroupInvitation invitation = validateInviteeAction(invitationId, userId);
        
        // Update status to ACCEPTED
        invitation.setStatus(GroupInvitationStatus.ACCEPTED);
        GroupInvitation savedInvitation = invitationRepository.save(invitation);
        
        // Add user to group as member
        User invitee = invitation.getInvitee();
        Group group = invitation.getGroup();
        GroupMember newMember = new GroupMember(group, invitee, GroupRole.MEMBER);
        groupMemberRepository.save(newMember);
        
        logger.info("Invitation {} accepted, user {} added to group {}", 
                   invitationId, userId, group.getId());
        return GroupInvitationResponse.fromEntity(savedInvitation);
    }

    @Override
    @Transactional
    public GroupInvitationResponse rejectInvitation(Long invitationId, Long userId) {
        logger.info("User {} rejecting invitation {}", userId, invitationId);
        
        GroupInvitation invitation = validateInviteeAction(invitationId, userId);
        
        // Update status to REJECTED
        invitation.setStatus(GroupInvitationStatus.REJECTED);
        GroupInvitation savedInvitation = invitationRepository.save(invitation);
        
        logger.info("Invitation {} rejected", invitationId);
        return GroupInvitationResponse.fromEntity(savedInvitation);
    }

    @Override
    public List<GroupInvitationResponse> getPendingInvitations(Long userId) {
        logger.debug("Getting pending invitations for user {}", userId);
        
        return invitationRepository.findPendingByInviteeId(userId).stream()
            .map(GroupInvitationResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<GroupInvitationResponse> getGroupInvitations(Long groupId, Long adminUserId) {
        logger.debug("Getting invitations for group {} by admin {}", groupId, adminUserId);
        
        // Validate admin permission
        permissionValidator.validateAdmin(groupId, adminUserId);
        
        return invitationRepository.findByGroupId(groupId).stream()
            .map(GroupInvitationResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public GroupInvitationResponse getInvitation(Long invitationId, Long userId) {
        logger.debug("Getting invitation {} for user {}", invitationId, userId);
        
        GroupInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new GroupInvitationNotFoundException(invitationId));
        
        // Verify the user is either inviter or invitee
        if (!invitation.getInviter().getId().equals(userId) && 
            !invitation.getInvitee().getId().equals(userId)) {
            throw new GroupInvitationNotFoundException(invitationId);
        }
        
        return GroupInvitationResponse.fromEntity(invitation);
    }

    @Override
    @Transactional
    public void cancelInvitation(Long invitationId, Long adminUserId) {
        logger.info("Admin {} canceling invitation {}", adminUserId, invitationId);
        
        GroupInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new GroupInvitationNotFoundException(invitationId));
        
        // Validate admin permission
        permissionValidator.validateAdmin(invitation.getGroup().getId(), adminUserId);
        
        // Only pending invitations can be canceled
        if (invitation.getStatus() != GroupInvitationStatus.PENDING) {
            throw new RuntimeException("Only pending invitations can be canceled");
        }
        
        // Delete the invitation
        invitationRepository.delete(invitation);
        
        logger.info("Invitation {} canceled", invitationId);
    }

    /**
     * Validates that a user can perform invitee actions on an invitation.
     * 
     * @param invitationId The invitation ID
     * @param userId The user ID attempting the action
     * @return The invitation if valid
     */
    private GroupInvitation validateInviteeAction(Long invitationId, Long userId) {
        GroupInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new GroupInvitationNotFoundException(invitationId));
        
        // Only the invitee can accept/reject
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new RuntimeException("Only the invitee can accept or reject an invitation");
        }
        
        // Only pending invitations can be accepted/rejected
        if (invitation.getStatus() != GroupInvitationStatus.PENDING) {
            throw new RuntimeException("Invitation has already been " + 
                                      invitation.getStatus().name().toLowerCase());
        }
        
        return invitation;
    }
}
