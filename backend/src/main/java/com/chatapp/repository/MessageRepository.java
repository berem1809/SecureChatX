package com.chatapp.repository;

import com.chatapp.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for Message entity.
 * Supports both direct (1-to-1) conversations and group conversations.
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ========================================================================
    // DIRECT CONVERSATION QUERIES
    // ========================================================================

    /**
     * Finds all messages in a direct conversation, ordered by creation time.
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Finds messages in a direct conversation with pagination.
     */
    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * Gets the latest message in a direct conversation.
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC LIMIT 1")
    Message findLatestByConversationId(@Param("conversationId") Long conversationId);

    /**
     * Counts messages in a direct conversation.
     */
    long countByConversationId(Long conversationId);

    // ========================================================================
    // GROUP CONVERSATION QUERIES
    // ========================================================================

    /**
     * Finds all messages in a group conversation, ordered by creation time.
     */
    List<Message> findByGroupIdOrderByCreatedAtAsc(Long groupId);

    /**
     * Finds messages in a group conversation with pagination.
     */
    Page<Message> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);

    /**
     * Gets the latest message in a group conversation.
     */
    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId ORDER BY m.createdAt DESC LIMIT 1")
    Message findLatestByGroupId(@Param("groupId") Long groupId);

    /**
     * Counts messages in a group conversation.
     */
    long countByGroupId(Long groupId);

    // ========================================================================
    // NOTIFICATION / UNREAD MESSAGE QUERIES
    // ========================================================================

    /**
     * Counts messages in a direct conversation not sent by the specified user.
     * Used for unread message counts.
     */
    long countByConversationIdAndSenderIdNot(Long conversationId, Long userId);

    /**
     * Counts messages in a group conversation not sent by the specified user.
     * Used for unread message counts.
     */
    long countByGroupIdAndSenderIdNot(Long groupId, Long userId);

    /**
     * Counts messages in a direct conversation created after a given timestamp
     * and not sent by the specified user. Used for unread count since last read.
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.sender.id != :userId AND m.createdAt > :lastReadAt")
    long countUnreadInConversation(@Param("conversationId") Long conversationId,
                                   @Param("userId") Long userId,
                                   @Param("lastReadAt") java.time.LocalDateTime lastReadAt);

    /**
     * Counts messages in a group conversation created after a given timestamp
     * and not sent by the specified user. Used for unread count since last read.
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.group.id = :groupId " +
           "AND m.sender.id != :userId AND m.createdAt > :lastReadAt")
    long countUnreadInGroup(@Param("groupId") Long groupId,
                            @Param("userId") Long userId,
                            @Param("lastReadAt") java.time.LocalDateTime lastReadAt);
}
