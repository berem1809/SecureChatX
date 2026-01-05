package com.chatapp.repository;

// ============================================================================
// IMPORTS
// ============================================================================

import com.chatapp.model.Group;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * GROUP REPOSITORY
 * ============================================================================
 * 
 * Provides database operations for group chats.
 * 
 * KEY QUERIES:
 * ------------
 * - Find groups created by a user
 * - Find groups a user is a member of
 * - Search groups by name
 */
public interface GroupRepository extends JpaRepository<Group, Long> {

    /**
     * Finds all groups created by a specific user.
     * 
     * @param createdBy The user who created the groups
     * @return List of groups
     */
    List<Group> findByCreatedBy(User createdBy);

    /**
     * Finds all groups created by a user ID.
     * 
     * @param userId The creator's user ID
     * @return List of groups
     */
    @Query("SELECT g FROM Group g WHERE g.createdBy.id = :userId")
    List<Group> findByCreatedById(@Param("userId") Long userId);

    /**
     * Finds all groups where the user is a member (not just creator).
     * This includes groups where the user is admin or member.
     * 
     * @param userId The user's ID
     * @return List of groups
     */
    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.user.id = :userId " +
           "ORDER BY g.lastMessageAt DESC NULLS LAST, g.createdAt DESC")
    List<Group> findGroupsByMemberId(@Param("userId") Long userId);

    /**
     * Finds a group by ID only if the user is a member.
     * This is used for authorization checks.
     * 
     * @param groupId The group ID
     * @param userId The user's ID who must be a member
     * @return Optional containing the group if found and user is member
     */
    @Query("SELECT g FROM Group g JOIN g.members m " +
           "WHERE g.id = :groupId AND m.user.id = :userId")
    Optional<Group> findByIdAndMemberId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * Searches for groups by name (case-insensitive).
     * 
     * @param namePattern The search pattern
     * @return List of matching groups
     */
    @Query("SELECT g FROM Group g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    List<Group> searchByName(@Param("namePattern") String namePattern);

    /**
     * Counts groups where user is a member.
     * 
     * @param userId The user's ID
     * @return Count of groups
     */
    @Query("SELECT COUNT(DISTINCT g) FROM Group g JOIN g.members m WHERE m.user.id = :userId")
    long countByMemberId(@Param("userId") Long userId);
}
