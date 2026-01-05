package com.chatapp.repository;

// ============================================================================
// IMPORTS
// ============================================================================

import com.chatapp.model.Group;
import com.chatapp.model.GroupInvitation;
import com.chatapp.model.GroupInvitationStatus;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * GROUP INVITATION REPOSITORY
 * ============================================================================
 * 
 * Provides database operations for group invitations.
 * 
 * KEY QUERIES:
 * ------------
 * - Find pending invitations for a user
 * - Find invitations for a group
 * - Check if user already has pending invitation to a group
 */
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    /**
     * Finds all invitations for a group.
     * 
     * @param group The group
     * @return List of invitations
     */
    List<GroupInvitation> findByGroup(Group group);

    /**
     * Finds all invitations for a group by group ID.
     * 
     * @param groupId The group ID
     * @return List of invitations
     */
    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.group.id = :groupId")
    List<GroupInvitation> findByGroupId(@Param("groupId") Long groupId);

    /**
     * Finds all invitations sent to a user.
     * 
     * @param invitee The invited user
     * @return List of invitations
     */
    List<GroupInvitation> findByInvitee(User invitee);

    /**
     * Finds all invitations sent to a user by user ID.
     * 
     * @param inviteeId The invitee's user ID
     * @return List of invitations
     */
    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.invitee.id = :inviteeId")
    List<GroupInvitation> findByInviteeId(@Param("inviteeId") Long inviteeId);

    /**
     * Finds all pending invitations for a user.
     * 
     * @param inviteeId The invitee's user ID
     * @return List of pending invitations
     */
    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.invitee.id = :inviteeId AND gi.status = 'PENDING'")
    List<GroupInvitation> findPendingByInviteeId(@Param("inviteeId") Long inviteeId);

    /**
     * Finds all pending invitations for a group.
     * 
     * @param groupId The group ID
     * @return List of pending invitations
     */
    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.group.id = :groupId AND gi.status = 'PENDING'")
    List<GroupInvitation> findPendingByGroupId(@Param("groupId") Long groupId);

    /**
     * Finds an invitation by group and invitee.
     * 
     * @param groupId The group ID
     * @param inviteeId The invitee's user ID
     * @return Optional containing the invitation if found
     */
    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.group.id = :groupId AND gi.invitee.id = :inviteeId")
    Optional<GroupInvitation> findByGroupIdAndInviteeId(@Param("groupId") Long groupId, 
                                                         @Param("inviteeId") Long inviteeId);

    /**
     * Checks if a pending invitation exists for a user to a group.
     * 
     * @param groupId The group ID
     * @param inviteeId The invitee's user ID
     * @return true if pending invitation exists
     */
    @Query("SELECT CASE WHEN COUNT(gi) > 0 THEN true ELSE false END FROM GroupInvitation gi " +
           "WHERE gi.group.id = :groupId AND gi.invitee.id = :inviteeId AND gi.status = 'PENDING'")
    boolean existsPendingByGroupIdAndInviteeId(@Param("groupId") Long groupId, 
                                                @Param("inviteeId") Long inviteeId);

    /**
     * Counts pending invitations for a user.
     * Useful for notification badges.
     * 
     * @param inviteeId The invitee's user ID
     * @return Count of pending invitations
     */
    @Query("SELECT COUNT(gi) FROM GroupInvitation gi WHERE gi.invitee.id = :inviteeId AND gi.status = 'PENDING'")
    long countPendingByInviteeId(@Param("inviteeId") Long inviteeId);

    /**
     * Deletes all invitations for a group.
     * Used when deleting a group.
     * 
     * @param group The group
     */
    void deleteByGroup(Group group);
}
