package com.chatapp.repository;

// ============================================================================
// IMPORTS
// ============================================================================

import com.chatapp.model.Conversation;
import com.chatapp.model.ConversationMember;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * CONVERSATION MEMBER REPOSITORY
 * ============================================================================
 * 
 * Provides database operations for conversation membership.
 * This is primarily used for features like read receipts and muting.
 */
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    /**
     * Finds all members of a conversation.
     * 
     * @param conversation The conversation
     * @return List of conversation members
     */
    List<ConversationMember> findByConversation(Conversation conversation);

    /**
     * Finds all conversation memberships for a user.
     * 
     * @param user The user
     * @return List of conversation members
     */
    List<ConversationMember> findByUser(User user);

    /**
     * Finds a specific membership by conversation and user.
     * 
     * @param conversationId The conversation ID
     * @param userId The user ID
     * @return Optional containing the membership if found
     */
    @Query("SELECT cm FROM ConversationMember cm " +
           "WHERE cm.conversation.id = :conversationId AND cm.user.id = :userId")
    Optional<ConversationMember> findByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                                                @Param("userId") Long userId);

    /**
     * Checks if a user is a member of a conversation.
     * 
     * @param conversationId The conversation ID
     * @param userId The user ID
     * @return true if user is a member
     */
    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END FROM ConversationMember cm " +
           "WHERE cm.conversation.id = :conversationId AND cm.user.id = :userId")
    boolean existsByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                            @Param("userId") Long userId);

    /**
     * Deletes all members of a conversation.
     * Used when deleting a conversation.
     * 
     * @param conversation The conversation
     */
    void deleteByConversation(Conversation conversation);
}
