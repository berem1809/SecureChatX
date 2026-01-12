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
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Finds all messages in a conversation, ordered by creation time.
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Finds messages in a conversation with pagination.
     */
    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * Gets the latest message in a conversation.
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC LIMIT 1")
    Message findLatestByConversationId(@Param("conversationId") Long conversationId);

    /**
     * Counts messages in a conversation.
     */
    long countByConversationId(Long conversationId);
}
