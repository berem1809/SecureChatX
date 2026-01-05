package com.chatapp.model;

// ============================================================================
// IMPORTS
// ============================================================================

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * CONVERSATION MEMBER ENTITY - Links users to conversations
 * ============================================================================
 * 
 * WHY THIS ENTITY?
 * ----------------
 * Although one-to-one conversations only have 2 users, this junction table:
 * 1. Makes membership queries faster (find all conversations for a user)
 * 2. Supports future features like read receipts, muting, pinning
 * 3. Provides a consistent pattern for both 1:1 and group chats
 * 
 * DATABASE TABLE:
 * ---------------
 * CREATE TABLE conversation_members (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     conversation_id BIGINT NOT NULL REFERENCES conversations(id),
 *     user_id BIGINT NOT NULL REFERENCES users(id),
 *     joined_at TIMESTAMP NOT NULL,
 *     last_read_at TIMESTAMP,
 *     is_muted BOOLEAN DEFAULT FALSE,
 *     UNIQUE(conversation_id, user_id)
 * );
 */
@Entity
@Table(
    name = "conversation_members",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"conversation_id", "user_id"},
        name = "uk_conversation_member"
    ),
    indexes = {
        @Index(name = "idx_conv_member_user", columnList = "user_id"),
        @Index(name = "idx_conv_member_conversation", columnList = "conversation_id")
    }
)
public class ConversationMember {

    // ========================================================================
    // PRIMARY KEY
    // ========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // RELATIONSHIPS
    // ========================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ========================================================================
    // MEMBER ATTRIBUTES
    // ========================================================================

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /**
     * Timestamp of when user last read messages in this conversation.
     * Used for unread message count.
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    /**
     * Whether the user has muted notifications for this conversation.
     */
    @Column(name = "is_muted")
    private boolean muted = false;

    // ========================================================================
    // JPA LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public ConversationMember() {}

    public ConversationMember(Conversation conversation, User user) {
        this.conversation = conversation;
        this.user = user;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }

    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
}
