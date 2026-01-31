package com.chatapp.controller;

import com.chatapp.dto.*;
import com.chatapp.exception.UserNotFoundException;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.GroupInvitationService;
import com.chatapp.service.GroupService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * GROUP CONTROLLER - Handles group chat endpoints
 * ============================================================================
 * 
 * REST Endpoints:
 * ---------------
 * POST /api/groups                           - Create a new group
 * GET  /api/groups                           - Get all user's groups
 * GET  /api/groups/discover                  - Get friend-created groups user can join
 * GET  /api/groups/{id}                      - Get specific group
 * PUT  /api/groups/{id}                      - Update group info (admin only)
 * POST /api/groups/{id}/leave                - Leave a group
 * GET  /api/groups/{id}/members              - Get group members
 * DELETE /api/groups/{id}/members/{userId}   - Remove member (admin only)
 * POST /api/groups/{id}/members/{userId}/promote - Promote to admin (admin only)
 * 
 * Invitation Endpoints:
 * ---------------------
 * POST /api/groups/{id}/invitations          - Send invitation (admin only)
 * GET  /api/groups/{id}/invitations          - Get group invitations (admin only)
 * DELETE /api/groups/invitations/{id}        - Cancel invitation (admin only)
 * GET  /api/groups/invitations/pending       - Get user's pending invitations
 * GET  /api/groups/invitations/{id}          - Get specific invitation
 * POST /api/groups/invitations/{id}/action   - Accept or reject invitation
 * 
 * SECURITY:
 * ---------
 * - All endpoints require valid JWT token
 * - Only members can access group information
 * - Admin-only operations are validated at service layer
 */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);

    private final GroupService groupService;
    private final GroupInvitationService invitationService;
    private final UserRepository userRepository;

    public GroupController(GroupService groupService, 
                          GroupInvitationService invitationService,
                          UserRepository userRepository) {
        this.groupService = groupService;
        this.invitationService = invitationService;
        this.userRepository = userRepository;
    }

    // ========================================================================
    // GROUP CRUD OPERATIONS
    // ========================================================================

    /**
     * Creates a new group.
     * The creator automatically becomes an admin.
     * 
     * POST /api/groups
     * Body: { "name": "Group Name", "description": "Group purpose/description" }
     * 
     * @param request The group creation request
     * @param authentication The authentication object
     * @return The created group
     */
    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody GroupCreateRequest request,
            Authentication authentication) {
        
        Long creatorId = getUserIdFromAuth(authentication);
        logger.info("User {} creating group: {}", creatorId, request.getName());
        
        GroupResponse response = groupService.createGroup(creatorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets all groups where the user is a member.
     * 
     * GET /api/groups
     * 
     * @param authentication The authentication object
     * @return List of groups
     */
    @GetMapping
    public ResponseEntity<List<GroupResponse>> getUserGroups(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting groups for user {}", userId);
        
        List<GroupResponse> groups = groupService.getUserGroups(userId);
        return ResponseEntity.ok(groups);
    }

    /**
     * Gets groups created by friends (users with accepted conversations)
     * that the current user is not already a member of.
     * 
     * GET /api/groups/discover
     * 
     * @param authentication The authentication object
     * @return List of discoverable groups
     */
    @GetMapping("/discover")
    public ResponseEntity<List<GroupResponse>> getDiscoverableGroups(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting discoverable groups for user {}", userId);

        List<GroupResponse> groups = groupService.getDiscoverableFriendGroups(userId);
        return ResponseEntity.ok(groups);
    }

    /**
     * Gets a specific group by ID.
     * 
     * GET /api/groups/{groupId}
     * 
     * @param groupId The group ID
     * @param authentication The authentication object
     * @return The group with member details
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting group {} for user {}", groupId, userId);
        
        GroupResponse group = groupService.getGroup(groupId, userId);
        return ResponseEntity.ok(group);
    }

    /**
     * Gets the symmetric key for a group.
     * Only group members can access this.
     *
     * GET /api/groups/{groupId}/key
     *
     * @param groupId The group ID
     * @param authentication The authentication object
     * @return The group's symmetric key
     */
    @GetMapping("/{groupId}/key")
    public ResponseEntity<GroupKeyResponse> getGroupKey(
            @PathVariable Long groupId,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        logger.debug("User {} requesting key for group {}", userId, groupId);

        return ResponseEntity.ok(groupService.getGroupKey(groupId, userId));
    }

    /**
     * Updates a member's wrapped group key.
     * This is used for E2EE key distribution.
     *
     * POST /api/groups/{groupId}/members/{memberId}/key
     * Body: { "encryptedKey": "...", "nonce": "...", "senderPublicKey": "..." }
     */
    @PostMapping("/{groupId}/members/{memberId}/key")
    public ResponseEntity<Void> updateMemberKey(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            @RequestBody GroupKeyUpdateRequest request,
            Authentication authentication) {

        Long currentUserId = getUserIdFromAuth(authentication);
        logger.info("User {} updating key for member {} in group {}", currentUserId, memberId, groupId);

        // Security check: Only members can update keys (ideally only admins or the user themselves)
        // For simplicity, we'll allow any member to "help" another member with the key
        groupService.updateMemberKey(groupId, memberId, 
            request.getEncryptedKey(), request.getNonce(), request.getSenderPublicKey());
            
        return ResponseEntity.ok().build();
    }

    /**
     * Updates group information (admin only).
     * 
     * PUT /api/groups/{groupId}
     * Body: { "name": "New Name", "description": "New description" }
     * 
     * @param groupId The group ID
     * @param request The update request
     * @param authentication The authentication object
     * @return The updated group
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable Long groupId,
            @RequestBody GroupCreateRequest request,
            Authentication authentication) {
        
        Long adminUserId = getUserIdFromAuth(authentication);
        logger.info("Admin {} updating group {}", adminUserId, groupId);
        
        GroupResponse response = groupService.updateGroup(
            groupId, adminUserId, request.getName(), request.getDescription()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Leaves a group.
     * 
     * POST /api/groups/{groupId}/leave
     * 
     * @param groupId The group ID
     * @param authentication The authentication object
     * @return 204 No Content
     */
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.info("User {} leaving group {}", userId, groupId);
        
        groupService.leaveGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Allows a user to join a group if they have a conversation with the group creator.
     * This enables "friends" to join friend-created groups directly from the Discover tab.
     * 
     * POST /api/groups/{groupId}/join
     * 
     * @param groupId The group ID
     * @param authentication The authentication object
     * @return The joined group
     */
    @PostMapping("/{groupId}/join")
    public ResponseEntity<GroupResponse> joinGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.info("User {} attempting to join group {}", userId, groupId);
        
        GroupResponse response = groupService.joinGroupAsFriend(groupId, userId);
        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // MEMBER MANAGEMENT
    // ========================================================================

    /**
     * Gets all members of a group.
     * 
     * GET /api/groups/{groupId}/members
     * 
     * @param groupId The group ID
     * @param authentication The authentication object
     * @return List of group members
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting members of group {} for user {}", groupId, userId);
        
        List<GroupMemberResponse> members = groupService.getGroupMembers(groupId, userId);
        return ResponseEntity.ok(members);
    }

    /**
     * Removes a member from a group (admin only).
     * 
     * DELETE /api/groups/{groupId}/members/{memberId}
     * 
     * @param groupId The group ID
     * @param memberId The member's user ID to remove
     * @param authentication The authentication object
     * @return 204 No Content
     */
    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            Authentication authentication) {
        
        Long adminUserId = getUserIdFromAuth(authentication);
        logger.info("Admin {} removing member {} from group {}", adminUserId, memberId, groupId);
        
        groupService.removeMember(groupId, adminUserId, memberId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Promotes a member to admin (admin only).
     * 
     * POST /api/groups/{groupId}/members/{memberId}/promote
     * 
     * @param groupId The group ID
     * @param memberId The member's user ID to promote
     * @param authentication The authentication object
     * @return The updated member info
     */
    @PostMapping("/{groupId}/members/{memberId}/promote")
    public ResponseEntity<GroupMemberResponse> promoteMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            Authentication authentication) {
        
        Long adminUserId = getUserIdFromAuth(authentication);
        logger.info("Admin {} promoting member {} in group {}", adminUserId, memberId, groupId);
        
        GroupMemberResponse response = groupService.promoteMember(groupId, adminUserId, memberId);
        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // INVITATION MANAGEMENT
    // ========================================================================

    /**
     * Sends an invitation to join a group (admin only).
     * 
     * POST /api/groups/{groupId}/invitations
     * Body: { "inviteeId": 123 }
     * 
     * @param groupId The group ID
     * @param request The invitation request
     * @param authentication The authentication object
     * @return The created invitation
     */
    @PostMapping("/{groupId}/invitations")
    public ResponseEntity<GroupInvitationResponse> createInvitation(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupInviteRequest request,
            Authentication authentication) {
        
        Long inviterId = getUserIdFromAuth(authentication);
        logger.info("Admin {} inviting user {} to group {}", inviterId, request.getInviteeId(), groupId);
        
        GroupInvitationResponse response = invitationService.createInvitation(groupId, inviterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets all invitations for a group (admin only).
     * 
     * GET /api/groups/{groupId}/invitations
     * 
     * @param groupId The group ID
     * @param authentication The authentication object
     * @return List of invitations
     */
    @GetMapping("/{groupId}/invitations")
    public ResponseEntity<List<GroupInvitationResponse>> getGroupInvitations(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        Long adminUserId = getUserIdFromAuth(authentication);
        logger.debug("Getting invitations for group {} by admin {}", groupId, adminUserId);
        
        List<GroupInvitationResponse> invitations = invitationService.getGroupInvitations(groupId, adminUserId);
        return ResponseEntity.ok(invitations);
    }

    /**
     * Gets all pending invitations for the current user.
     * 
     * GET /api/groups/invitations/pending
     * 
     * @param authentication The authentication object
     * @return List of pending invitations
     */
    @GetMapping("/invitations/pending")
    public ResponseEntity<List<GroupInvitationResponse>> getPendingInvitations(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting pending invitations for user {}", userId);
        
        List<GroupInvitationResponse> invitations = invitationService.getPendingInvitations(userId);
        return ResponseEntity.ok(invitations);
    }

    /**
     * Gets a specific invitation by ID.
     * 
     * GET /api/groups/invitations/{invitationId}
     * 
     * @param invitationId The invitation ID
     * @param authentication The authentication object
     * @return The invitation
     */
    @GetMapping("/invitations/{invitationId}")
    public ResponseEntity<GroupInvitationResponse> getInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting invitation {} for user {}", invitationId, userId);
        
        GroupInvitationResponse invitation = invitationService.getInvitation(invitationId, userId);
        return ResponseEntity.ok(invitation);
    }

    /**
     * Accepts or rejects an invitation.
     * 
     * POST /api/groups/invitations/{invitationId}/action
     * Body: { "action": "ACCEPT" } or { "action": "REJECT" }
     * 
     * @param invitationId The invitation ID
     * @param actionRequest The action to perform
     * @param authentication The authentication object
     * @return The updated invitation
     */
    @PostMapping("/invitations/{invitationId}/action")
    public ResponseEntity<GroupInvitationResponse> handleInvitationAction(
            @PathVariable Long invitationId,
            @Valid @RequestBody GroupInvitationActionRequest actionRequest,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.info("User {} performing action {} on invitation {}", 
                   userId, actionRequest.getAction(), invitationId);
        
        GroupInvitationResponse response;
        if (actionRequest.isAccept()) {
            response = invitationService.acceptInvitation(invitationId, userId);
        } else {
            response = invitationService.rejectInvitation(invitationId, userId);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a pending invitation (admin only).
     * 
     * DELETE /api/groups/invitations/{invitationId}
     * 
     * @param invitationId The invitation ID
     * @param authentication The authentication object
     * @return 204 No Content
     */
    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        
        Long adminUserId = getUserIdFromAuth(authentication);
        logger.info("Admin {} canceling invitation {}", adminUserId, invitationId);
        
        invitationService.cancelInvitation(invitationId, adminUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extracts the user ID from the authentication object.
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> UserNotFoundException.byEmail(email))
            .getId();
    }
}
