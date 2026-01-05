package com.chatapp.repository;

// ============================================================================
// IMPORTS
// ============================================================================

import com.chatapp.model.Conversation;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * CONVERSATION REPOSITORY
 * ============================================================================
 * 
 * Provides database operations for one-to-one conversations.
 * 
 * KEY QUERIES:
 * ------------
 * - Find all conversations for a user
 * - Check if conversation exists between two users
 * - Find conversation between specific users
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Finds all conversations where a user is a participant.
     * 
     * @param userId The user's ID
     * @return List of conversations
     */
    @Query("SELECT c FROM Conversation c WHERE c.user1.id = :userId OR c.user2.id = :userId " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC")
    List<Conversation> findByUserId(@Param("userId") Long userId);

    /**
     * Checks if a conversation already exists between two users.
     * Since we normalize user IDs (user1Id < user2Id), we check both orders.
     * 
     * @param user1Id First user's ID
     * @param user2Id Second user's ID
     * @return true if conversation exists
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Conversation c " +
           "WHERE (c.user1.id = :user1Id AND c.user2.id = :user2Id) " +
           "OR (c.user1.id = :user2Id AND c.user2.id = :user1Id)")
    boolean existsBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    /**
     * Finds a conversation between two specific users.
     * Handles both orderings since we normalize IDs.
     * 
     * @param user1Id First user's ID
     * @param user2Id Second user's ID
     * @return Optional containing the conversation if found
     */
    @Query("SELECT c FROM Conversation c " +
           "WHERE (c.user1.id = :user1Id AND c.user2.id = :user2Id) " +
           "OR (c.user1.id = :user2Id AND c.user2.id = :user1Id)")
    Optional<Conversation> findBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    /**
     * Finds a conversation by its ID only if the user is a participant.
     * This is used for authorization checks.
     * 
     * @param conversationId The conversation ID
     * @param userId The user's ID who must be a participant
     * @return Optional containing the conversation if found and user is participant
     */
    @Query("SELECT c FROM Conversation c WHERE c.id = :conversationId " +
           "AND (c.user1.id = :userId OR c.user2.id = :userId)")
    Optional<Conversation> findByIdAndUserId(@Param("conversationId") Long conversationId, 
                                              @Param("userId") Long userId);

    /**
     * Counts conversations for a user.
     * 
     * @param userId The user's ID
     * @return Count of conversations
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.user1.id = :userId OR c.user2.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
