package com.chatapp.repository;

// ============================================================================
// IMPORTS
// ============================================================================

import com.chatapp.model.Group;
import com.chatapp.model.GroupMember;
import com.chatapp.model.GroupRole;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * GROUP MEMBER REPOSITORY
 * ============================================================================
 * 
 * Provides database operations for group membership.
 * 
 * KEY QUERIES:
 * ------------
 * - Find members of a group
 * - Check if user is member/admin of a group
 * - Find user's role in a group
 */
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    /**
     * Finds all members of a group.
     * 
     * @param group The group
     * @return List of group members
     */
    List<GroupMember> findByGroup(Group group);

    /**
     * Finds all members of a group by group ID.
     * 
     * @param groupId The group ID
     * @return List of group members
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId")
    List<GroupMember> findByGroupId(@Param("groupId") Long groupId);

    /**
     * Finds all group memberships for a user.
     * 
     * @param user The user
     * @return List of group memberships
     */
    List<GroupMember> findByUser(User user);

    /**
     * Finds all group memberships for a user by user ID.
     * 
     * @param userId The user ID
     * @return List of group memberships
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.user.id = :userId")
    List<GroupMember> findByUserId(@Param("userId") Long userId);

    /**
     * Finds a specific membership by group and user.
     * 
     * @param groupId The group ID
     * @param userId The user ID
     * @return Optional containing the membership if found
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    Optional<GroupMember> findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * Checks if a user is a member of a group.
     * 
     * @param groupId The group ID
     * @param userId The user ID
     * @return true if user is a member
     */
    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END FROM GroupMember gm " +
           "WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    boolean existsByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * Checks if a user is an admin of a group.
     * 
     * @param groupId The group ID
     * @param userId The user ID
     * @return true if user is an admin
     */
    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END FROM GroupMember gm " +
           "WHERE gm.group.id = :groupId AND gm.user.id = :userId AND gm.role = 'ADMIN'")
    boolean isAdmin(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * Finds all admins of a group.
     * 
     * @param groupId The group ID
     * @return List of admin members
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.role = 'ADMIN'")
    List<GroupMember> findAdminsByGroupId(@Param("groupId") Long groupId);

    /**
     * Counts members in a group.
     * 
     * @param groupId The group ID
     * @return Count of members
     */
    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId")
    long countByGroupId(@Param("groupId") Long groupId);

    /**
     * Counts admins in a group.
     * 
     * @param groupId The group ID
     * @return Count of admins
     */
    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.role = 'ADMIN'")
    long countAdminsByGroupId(@Param("groupId") Long groupId);

    /**
     * Deletes a membership by group ID and user ID.
     * 
     * @param groupId The group ID
     * @param userId The user ID
     */
    @Query("DELETE FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
